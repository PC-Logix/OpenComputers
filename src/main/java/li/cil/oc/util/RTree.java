package li.cil.oc.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** A spatial index for point values with three-dimensional range queries. */
public final class RTree<T> {
    private final int maxEntries;
    private final int minEntries;
    private final Function<T, Point> coordinate;
    private final Map<T, Leaf> entries = new HashMap<>();
    private NonLeaf root = new NonLeaf();

    public RTree(int maxEntries, Function<T, Point> coordinate) {
        if (maxEntries < 2) throw new IllegalArgumentException("maxEntries must be larger or equal to 2.");
        this.maxEntries = maxEntries;
        this.minEntries = Math.max(maxEntries / 2, 1);
        this.coordinate = coordinate;
    }

    public synchronized Optional<Point> get(T value) {
        Leaf entry = entries.get(value);
        return entry == null ? Optional.empty() : Optional.of(entry.bounds.min);
    }

    public synchronized List<Bounds> allBounds() {
        return root.allBounds(0);
    }

    public synchronized boolean add(T value) {
        boolean replaced = remove(value);
        Leaf entry = new Leaf(value, coordinate.apply(value));
        entries.put(value, entry);
        Node newNode = root.add(entry);
        if (newNode != root) root = new NonLeaf(newNode, root);
        return !replaced;
    }

    public synchronized boolean remove(T value) {
        Leaf entry = entries.remove(value);
        if (entry == null) return false;
        Node change = root.remove(entry);
        assert change == entry || change == root;
        if (root.children.size() == 1 && root.children.iterator().next() instanceof NonLeaf child) {
            root = child;
        } else {
            root.bounds = around(root.children);
        }
        return true;
    }

    public synchronized List<T> query(Point from, Point to) {
        return root.query(new Rectangle(from, to));
    }

    private Rectangle around(Set<Node> values) {
        Point minimum = Point.POSITIVE_INFINITY;
        Point maximum = Point.NEGATIVE_INFINITY;
        for (Node value : values) {
            minimum = value.bounds.min.min(minimum);
            maximum = value.bounds.max.max(maximum);
        }
        return new Rectangle(minimum, maximum);
    }

    public record Point(double x, double y, double z) {
        private static final Point NEGATIVE_INFINITY = new Point(
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        private static final Point POSITIVE_INFINITY = new Point(
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

        private Point min(Point other) {
            return new Point(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
        }

        private Point max(Point other) {
            return new Point(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
        }
    }

    public record Bounds(Point min, Point max, int level) {
    }

    private final class Rectangle {
        private final Point min;
        private final Point max;

        private Rectangle(Point min, Point max) {
            this.min = min;
            this.max = max;
        }

        private Rectangle including(Rectangle other) {
            return new Rectangle(min.min(other.min), max.max(other.max));
        }

        private boolean intersects(Rectangle other) {
            return other.min.x <= max.x && other.min.y <= max.y && other.min.z <= max.z
                && other.max.x >= min.x && other.max.y >= min.y && other.max.z >= min.z;
        }

        private double volume() {
            return (max.x - min.x) * (max.y - min.y) * (max.z - min.z);
        }

    }

    private abstract class Node {
        protected Rectangle bounds;

        protected List<Bounds> allBounds(int level) {
            return new ArrayList<>(List.of(new Bounds(bounds.min, bounds.max, level)));
        }

        boolean isLeaf() {
            return true;
        }

        abstract Node add(Node value);

        abstract Node remove(Node value);

        abstract List<T> query(Rectangle query);
    }

    private final class NonLeaf extends Node {
        private final Set<Node> children = new HashSet<>();

        private NonLeaf(Node... nodes) {
            bounds = new Rectangle(Point.POSITIVE_INFINITY, Point.NEGATIVE_INFINITY);
            for (Node child : nodes) {
                children.add(child);
                bounds = bounds.including(child.bounds);
            }
        }

        @Override
        protected List<Bounds> allBounds(int level) {
            List<Bounds> result = super.allBounds(level);
            for (Node child : children) result.addAll(child.allBounds(level + 1));
            return result;
        }

        @Override
        boolean isLeaf() {
            return !children.isEmpty() && children.iterator().next() instanceof Leaf;
        }

        @Override
        Node add(Node value) {
            assert value != this;
            uncheckedAdd(value);
            if (children.size() > maxEntries) return split();
            bounds = bounds.including(value.bounds);
            return this;
        }

        private void uncheckedAdd(Node value) {
            Node bestChild = null;
            double bestGrowth = Double.POSITIVE_INFINITY;
            double bestVolume = Double.POSITIVE_INFINITY;
            for (Node child : children) {
                if (child.isLeaf() && !(value instanceof Leaf)) continue;
                double oldVolume = child.bounds.volume();
                double volume = child.bounds.including(value.bounds).volume();
                double growth = volume - oldVolume;
                if (growth < bestGrowth || (growth == bestGrowth && volume < bestVolume)) {
                    bestChild = child;
                    bestGrowth = growth;
                    bestVolume = volume;
                }
            }
            children.add(bestChild == null ? value : bestChild.add(value));
        }

        @Override
        Node remove(Node value) {
            if (!bounds.intersects(value.bounds)) return null;
            for (Node child : new ArrayList<>(children)) {
                Node change = child.remove(value);
                if (change == null) continue;
                if (change == child) {
                    children.remove(child);
                    if (child instanceof NonLeaf nonLeaf) {
                        for (Node descendant : nonLeaf.children) uncheckedAdd(descendant);
                        if (children.size() > maxEntries) return split();
                    } else {
                        assert child == value;
                    }
                    if (children.size() < minEntries) return this;
                    bounds = around(children);
                    return value;
                }
                if (change == value) {
                    bounds = around(children);
                    return value;
                }
                assert change instanceof NonLeaf;
                uncheckedAdd(change);
                if (children.size() > maxEntries) return split();
                bounds = around(children);
                return value;
            }
            return null;
        }

        @Override
        List<T> query(Rectangle query) {
            List<T> result = new ArrayList<>();
            if (query.intersects(bounds)) {
                for (Node child : children) result.addAll(child.query(query));
            }
            return result;
        }

        private Node split() {
            List<Node> values = new ArrayList<>(children);
            Node seed1 = null;
            Node seed2 = null;
            double worst = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < values.size(); i++) {
                Node first = values.get(i);
                for (int j = i + 1; j < values.size(); j++) {
                    Node second = values.get(j);
                    double volume = first.bounds.including(second.bounds).volume();
                    double waste = volume - first.bounds.volume() - second.bounds.volume();
                    if (waste > worst) {
                        seed1 = first;
                        seed2 = second;
                        worst = waste;
                    }
                }
            }
            if (seed1 == null || seed2 == null) throw new AssertionError();

            SplitResult first = new SplitResult(seed1);
            SplitResult second = new SplitResult(seed2);
            Set<Node> remaining = new HashSet<>(values);
            remaining.remove(seed1);
            remaining.remove(seed2);
            while (!remaining.isEmpty()) {
                if (minEntries - first.set.size() >= remaining.size()) {
                    for (Node value : remaining) first.add(value);
                    remaining.clear();
                } else if (minEntries - second.set.size() >= remaining.size()) {
                    for (Node value : remaining) second.add(value);
                    remaining.clear();
                } else {
                    Node bestValue = null;
                    SplitResult target = first;
                    double best = Double.NEGATIVE_INFINITY;
                    for (Node value : remaining) {
                        double volume1 = first.volumeIncluding(value);
                        double volume2 = second.volumeIncluding(value);
                        double growth1 = volume1 - first.volume();
                        double growth2 = volume2 - second.volume();
                        double difference = Math.abs(growth2 - growth1);
                        if (difference > best) {
                            bestValue = value;
                            target = growth1 < growth2 || (growth1 == growth2 && volume1 < volume2)
                                ? first : second;
                            best = difference;
                        }
                    }
                    if (bestValue == null) throw new AssertionError();
                    remaining.remove(bestValue);
                    target.add(bestValue);
                }
            }

            children.clear();
            children.addAll(first.set);
            bounds = first.bounds;

            NonLeaf sibling = new NonLeaf();
            sibling.children.addAll(second.set);
            sibling.bounds = second.bounds;
            return sibling;
        }
    }

    private final class Leaf extends Node {
        private final T data;

        private Leaf(T data, Point point) {
            this.data = data;
            bounds = new Rectangle(point, point);
        }

        @Override
        Node add(Node value) {
            return value;
        }

        @Override
        Node remove(Node value) {
            return value == this ? this : null;
        }

        @Override
        List<T> query(Rectangle query) {
            return query.intersects(bounds) ? List.of(data) : List.of();
        }
    }

    private final class SplitResult {
        private final Set<Node> set = new HashSet<>();
        private Rectangle bounds;

        private SplitResult(Node seed) {
            set.add(seed);
            bounds = seed.bounds;
        }

        private void add(Node value) {
            set.add(value);
            bounds = bounds.including(value.bounds);
        }

        private double volume() {
            return bounds.volume();
        }

        private double volumeIncluding(Node value) {
            return bounds.including(value.bounds).volume();
        }
    }
}
