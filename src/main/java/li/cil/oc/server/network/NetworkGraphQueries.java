package li.cil.oc.server.network;

import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import scala.Option;
import scala.collection.Iterable;
import scala.collection.Iterator;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.Map;

import java.util.HashSet;
import java.util.Set;

/** Node lookup and reachability queries over a network graph. */
public final class NetworkGraphQueries {
    private NetworkGraphQueries() {}

    public static Node node(Map<String, NetworkVertex> data, String address) {
        Option<NetworkVertex> vertex = data.get(address);
        return vertex.isDefined() ? vertex.get().data : null;
    }

    public static Iterable<Node> nodes(Map<String, NetworkVertex> data) {
        ArrayBuffer<Node> result = new ArrayBuffer<>();
        Iterator<NetworkVertex> vertices = data.values().iterator();
        while (vertices.hasNext()) result.addOne(vertices.next().data);
        return result;
    }

    public static Iterable<Node> reachableNodes(Map<String, NetworkVertex> data, Node reference) {
        Set<Node> referenceNeighbors = toJavaSet(neighbors(data, reference));
        ArrayBuffer<Node> result = new ArrayBuffer<>();
        Iterator<NetworkVertex> vertices = data.values().iterator();
        while (vertices.hasNext()) {
            Node node = vertices.next().data;
            if (!node.equals(reference) && (node.reachability() == Visibility.Network
                    || (node.reachability() == Visibility.Neighbors && referenceNeighbors.contains(node)))) {
                result.addOne(node);
            }
        }
        return result;
    }

    public static Iterable<Node> reachingNodes(Map<String, NetworkVertex> data, Node reference) {
        Visibility reachability = reference.reachability();
        if (reachability == Visibility.None) return new ArrayBuffer<>();

        Set<Node> referenceNeighbors = reachability == Visibility.Neighbors
                ? toJavaSet(neighbors(data, reference))
                : java.util.Collections.emptySet();
        ArrayBuffer<Node> result = new ArrayBuffer<>();
        Iterator<NetworkVertex> vertices = data.values().iterator();
        while (vertices.hasNext()) {
            Node node = vertices.next().data;
            if (!node.equals(reference)
                    && (reachability == Visibility.Network || referenceNeighbors.contains(node))) {
                result.addOne(node);
            }
        }
        return result;
    }

    public static Iterable<Node> neighbors(Map<String, NetworkVertex> data, Node node) {
        Option<NetworkVertex> found = data.get(node.address());
        if (found.isEmpty() || !found.get().data.equals(node)) {
            throw new IllegalArgumentException("Node must be in this network.");
        }

        NetworkVertex vertex = found.get();
        ArrayBuffer<Node> result = new ArrayBuffer<>();
        Iterator<NetworkEdge> edges = vertex.edges.iterator();
        while (edges.hasNext()) result.addOne(edges.next().other(vertex).data);
        return result;
    }

    private static Set<Node> toJavaSet(Iterable<Node> nodes) {
        Set<Node> result = new HashSet<>();
        Iterator<Node> iterator = nodes.iterator();
        while (iterator.hasNext()) result.add(iterator.next());
        return result;
    }
}
