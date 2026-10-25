package li.cil.oc.server.network;

import scala.Option;
import scala.collection.Iterator;
import scala.collection.Seq;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.Map;

import java.util.Objects;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Stateful graph operations for address remapping, disconnects, and node removal. */
public final class NetworkGraphMutations {
    private NetworkGraphMutations() {}

    public static boolean connect(Map<String, NetworkVertex> data, NetworkWrapper wrapper,
                                  Node nodeA, Node nodeB,
                                  BiFunction<NetworkVertex, Node, Boolean> add) {
        if (nodeA == null) throw new NullPointerException("nodeA");
        if (nodeB == null) throw new NullPointerException("nodeB");
        if (Objects.equals(nodeA, nodeB)) {
            throw new IllegalArgumentException("Cannot connect a node to itself.");
        }

        boolean containsA = contains(data, wrapper, nodeA);
        boolean containsB = contains(data, wrapper, nodeB);
        if (!containsA && !containsB) {
            throw new IllegalArgumentException("At least one of the nodes must already be in this network.");
        }

        NetworkVertex oldNodeA = containsA ? data.apply(nodeA.address()) : null;
        NetworkVertex oldNodeB = containsB ? data.apply(nodeB.address()) : null;
        if (containsA && containsB) {
            Iterator<NetworkEdge> edges = oldNodeA.edges.iterator();
            while (edges.hasNext()) {
                if (edges.next().isBetween(oldNodeA, oldNodeB)) return false;
            }
            assert !hasEdge(oldNodeB, oldNodeA);

            new NetworkEdge(oldNodeA, oldNodeB);
            if (oldNodeA.data.reachability() == li.cil.oc.api.network.Visibility.Neighbors) {
                oldNodeB.data.onConnect(oldNodeA.data);
            }
            if (oldNodeB.data.reachability() == li.cil.oc.api.network.Visibility.Neighbors) {
                oldNodeA.data.onConnect(oldNodeB.data);
            }
            return true;
        }
        return containsA ? add.apply(oldNodeA, nodeB) : add.apply(oldNodeB, nodeA);
    }

    private static boolean hasEdge(NetworkVertex from, NetworkVertex to) {
        Iterator<NetworkEdge> edges = from.edges.iterator();
        while (edges.hasNext()) if (edges.next().isBetween(from, to)) return true;
        return false;
    }

    public static boolean add(NetworkCore network, Map<String, NetworkVertex> data,
                              ArrayBuffer<Connector> connectors, NetworkWrapper wrapper,
                              NetworkVertex oldNode, Node addedNode,
                              Function<Node, NetworkVertex> addNew,
                              BiFunction<NetworkVertex, Node, Boolean> add) {
        List<ConnectCallback> callbacks = new ArrayList<>();
        if (addedNode.network() == null) {
            NetworkVertex newNode = addNew.apply(addedNode);
            new NetworkEdge(oldNode, newNode);
            switch (addedNode.reachability()) {
                case None -> queue(callbacks, addedNode, List.of(addedNode));
                case Neighbors -> {
                    queue(callbacks, addedNode, concat(List.of(addedNode), network.neighbors(addedNode)));
                    queueReaching(callbacks, network.reachingNodes(addedNode), addedNode);
                }
                case Network -> {
                    queue(callbacks, addedNode, excluding(network.nodes(), addedNode));
                    queueReaching(callbacks, network.reachingNodes(addedNode), addedNode);
                }
            }

            addedNode.onConnect(addedNode);
            scala.collection.Iterable<li.cil.oc.api.network.Node> currentNodes = network.nodes();
            List<li.cil.oc.api.network.Node> allNodes = toJavaList(currentNodes);
            Iterator<li.cil.oc.api.network.Node> visible = currentNodes.iterator();
            while (visible.hasNext()) {
                li.cil.oc.api.network.Node node = visible.next();
                if (node.reachability() == li.cil.oc.api.network.Visibility.Network) {
                    queue(callbacks, node, allNodes);
                }
            }
        } else {
            NetworkCore otherNetwork = ((NetworkWrapper) addedNode.network()).network();
            List<NetworkVertex> duplicates = new ArrayList<>();
            Iterator<scala.Tuple2<String, NetworkVertex>> entries = otherNetwork.graphData().iterator();
            while (entries.hasNext()) {
                scala.Tuple2<String, NetworkVertex> entry = entries.next();
                if (data.contains(entry._1())) duplicates.add(entry._2());
            }

            NetworkCore otherNetworkAfterReaddress;
            if (duplicates.isEmpty()) {
                otherNetworkAfterReaddress = otherNetwork;
            } else {
                for (NetworkVertex vertex : duplicates) {
                    Node node = vertex.data;
                    List<Node> neighbors = new ArrayList<>();
                    Iterator<NetworkEdge> edges = vertex.edges.iterator();
                    while (edges.hasNext()) neighbors.add(edges.next().other(vertex).data);

                    String newAddress;
                    do {
                        newAddress = UUID.randomUUID().toString();
                    } while (data.contains(newAddress) || otherNetwork.graphData().contains(newAddress));

                    node.remove();
                    node.address_$eq(newAddress);
                    Network.joinNewNetwork(node);

                    if (newAddress.equals(node.address())) {
                        for (Node neighbor : neighbors) {
                            if (neighbor.network() != null) neighbor.connect(node);
                        }
                    } else {
                        li.cil.oc.OpenComputers.log().error("I can't see this happening any other way than someone directly setting node addresses, which they shouldn't. So yeah. Shit'll be borked. Deal with it.");
                        node.remove();
                    }
                }
                otherNetworkAfterReaddress = ((NetworkWrapper) duplicates.get(0).data.network()).network();
            }

            if (addedNode.network() != null
                    && ((NetworkWrapper) addedNode.network()).network() == otherNetworkAfterReaddress) {
                if (addedNode.reachability() == li.cil.oc.api.network.Visibility.Neighbors) {
                    queue(callbacks, addedNode, List.of(oldNode.data));
                }
                if (oldNode.data.reachability() == li.cil.oc.api.network.Visibility.Neighbors) {
                    queue(callbacks, oldNode.data, List.of(addedNode));
                }

                List<li.cil.oc.api.network.Node> oldNodes = toJavaList(network.nodes());
                List<li.cil.oc.api.network.Node> newNodes = toJavaList(otherNetworkAfterReaddress.nodes());
                List<li.cil.oc.api.network.Node> oldVisibleNodes = visible(oldNodes);
                List<li.cil.oc.api.network.Node> newVisibleNodes = visible(newNodes);
                for (li.cil.oc.api.network.Node node : newVisibleNodes) queue(callbacks, node, oldNodes);
                for (li.cil.oc.api.network.Node node : oldVisibleNodes) queue(callbacks, node, newNodes);

                Iterator<scala.Tuple2<String, NetworkVertex>> newEntries = otherNetworkAfterReaddress.graphData().iterator();
                while (newEntries.hasNext()) {
                    scala.Tuple2<String, NetworkVertex> entry = newEntries.next();
                    data.update(entry._1(), entry._2());
                }
                Iterator<Connector> newConnectors = otherNetworkAfterReaddress.graphConnectors().iterator();
                while (newConnectors.hasNext()) connectors.addOne(newConnectors.next());
                network.globalBuffer_$eq(network.globalBuffer() + otherNetworkAfterReaddress.globalBuffer());
                network.globalBufferSize_$eq(network.globalBufferSize() + otherNetworkAfterReaddress.globalBufferSize());
                Iterator<NetworkVertex> vertices = otherNetworkAfterReaddress.graphData().values().iterator();
                while (vertices.hasNext()) {
                    Node node = vertices.next().data;
                    if (node instanceof Connector connector) {
                        connector.distributor_$eq(scala.Option$.MODULE$.apply(wrapper));
                    }
                    node.network_$eq(wrapper);
                }
                otherNetworkAfterReaddress.graphData().clear();
                otherNetworkAfterReaddress.graphConnectors().clear();

                new NetworkEdge(oldNode, data.apply(addedNode.address()));
            } else {
                return add.apply(oldNode, addedNode);
            }
        }

        for (ConnectCallback callback : callbacks) callback.target.onConnect(callback.other);
        return true;
    }

    private static List<li.cil.oc.api.network.Node> concat(List<Node> first,
                                                           scala.collection.Iterable<li.cil.oc.api.network.Node> second) {
        List<li.cil.oc.api.network.Node> result = new ArrayList<>(first);
        Iterator<li.cil.oc.api.network.Node> iterator = second.iterator();
        while (iterator.hasNext()) result.add(iterator.next());
        return result;
    }

    private static List<li.cil.oc.api.network.Node> excluding(
            scala.collection.Iterable<li.cil.oc.api.network.Node> nodes, Node excluded) {
        List<li.cil.oc.api.network.Node> result = new ArrayList<>();
        Iterator<li.cil.oc.api.network.Node> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            li.cil.oc.api.network.Node node = iterator.next();
            if (!node.equals(excluded)) result.add(node);
        }
        result.add(0, excluded);
        return result;
    }

    private static void queueReaching(List<ConnectCallback> callbacks,
                                      scala.collection.Iterable<li.cil.oc.api.network.Node> nodes,
                                      Node other) {
        Iterator<li.cil.oc.api.network.Node> iterator = nodes.iterator();
        while (iterator.hasNext()) queue(callbacks, iterator.next(), List.of(other));
    }

    private static void queue(List<ConnectCallback> callbacks, li.cil.oc.api.network.Node target,
                              Iterable<li.cil.oc.api.network.Node> others) {
        for (li.cil.oc.api.network.Node other : others) {
            callbacks.add(new ConnectCallback((Node) target, other));
        }
    }

    private static List<li.cil.oc.api.network.Node> toJavaList(
            scala.collection.Iterable<li.cil.oc.api.network.Node> nodes) {
        List<li.cil.oc.api.network.Node> result = new ArrayList<>();
        Iterator<li.cil.oc.api.network.Node> iterator = nodes.iterator();
        while (iterator.hasNext()) result.add(iterator.next());
        return result;
    }

    private static List<li.cil.oc.api.network.Node> visible(List<li.cil.oc.api.network.Node> nodes) {
        List<li.cil.oc.api.network.Node> result = new ArrayList<>();
        for (li.cil.oc.api.network.Node node : nodes) {
            if (node.reachability() == li.cil.oc.api.network.Visibility.Network) result.add(node);
        }
        return result;
    }

    private static final class ConnectCallback {
        private final Node target;
        private final li.cil.oc.api.network.Node other;

        private ConnectCallback(Node target, li.cil.oc.api.network.Node other) {
            this.target = target;
            this.other = other;
        }
    }

    public static void remap(Map<String, NetworkVertex> data, Node remappedNode, String newAddress,
                             Function<Node, NetworkVertex> addNew) {
        Option<NetworkVertex> found = data.get(remappedNode.address());
        if (found.isEmpty()) throw new AssertionError("Node believes it belongs to a network it doesn't.");

        NetworkVertex vertex = found.get();
        ArrayBuffer<NetworkVertex> neighbors = new ArrayBuffer<>();
        Iterator<NetworkEdge> edges = vertex.edges.iterator();
        while (edges.hasNext()) neighbors.addOne(edges.next().other(vertex));

        Node node = vertex.data;
        node.remove();
        node.address_$eq(newAddress);
        while (data.contains(node.address())) node.address_$eq(UUID.randomUUID().toString());
        if (neighbors.isEmpty()) addNew.apply(node);
        else {
            Iterator<NetworkVertex> iterator = neighbors.iterator();
            while (iterator.hasNext()) iterator.next().data.connect(node);
        }
    }

    public static boolean disconnect(NetworkCore network, Map<String, NetworkVertex> data,
                                    NetworkWrapper wrapper, Node nodeA, Node nodeB) {
        if (Objects.equals(nodeA, nodeB)) throw new IllegalArgumentException("Cannot disconnect a node from itself.");
        if (!contains(data, wrapper, nodeA) || !contains(data, wrapper, nodeB)) {
            throw new IllegalArgumentException("Both of the nodes must be in this network.");
        }

        NetworkVertex oldNodeA = data.apply(nodeA.address());
        NetworkVertex oldNodeB = data.apply(nodeB.address());
        Iterator<NetworkEdge> edges = oldNodeA.edges.iterator();
        while (edges.hasNext()) {
            NetworkEdge edge = edges.next();
            if (!edge.isBetween(oldNodeA, oldNodeB)) continue;

            network.handleSplit(removeEdge(edge));
            if (edge.left.data.reachability() == li.cil.oc.api.network.Visibility.Neighbors) {
                edge.right.data.onDisconnect(edge.left.data);
            }
            if (edge.right.data.reachability() == li.cil.oc.api.network.Visibility.Neighbors) {
                edge.left.data.onDisconnect(edge.right.data);
            }
            return true;
        }
        return false;
    }

    public static boolean remove(NetworkCore network, Map<String, NetworkVertex> data,
                                 ArrayBuffer<Connector> connectors, NetworkWrapper wrapper, Node node) {
        Option<NetworkVertex> found = data.remove(node.address());
        if (found.isEmpty()) return false;

        NetworkVertex entry = found.get();
        if (node instanceof Connector connector) network.removeConnector(connector);
        node.network_$eq(null);
        Seq<Map<String, NetworkVertex>> subGraphs = removeVertex(entry);

        ArrayBuffer<Node> targets = new ArrayBuffer<>();
        targets.addOne(node);
        switch (entry.data.reachability()) {
            case None -> { }
            case Neighbors -> {
                Iterator<NetworkEdge> edges = entry.edges.iterator();
                while (edges.hasNext()) targets.addOne(edges.next().other(entry).data);
            }
            case Network -> {
                Iterator<Map<String, NetworkVertex>> graphs = subGraphs.iterator();
                while (graphs.hasNext()) {
                    Iterator<NetworkVertex> vertices = graphs.next().values().iterator();
                    while (vertices.hasNext()) targets.addOne(vertices.next().data);
                }
            }
        }

        network.handleSplit(subGraphs);
        Iterator<Node> iterator = targets.iterator();
        while (iterator.hasNext()) iterator.next().onDisconnect(node);
        return true;
    }

    private static boolean contains(Map<String, NetworkVertex> data, NetworkWrapper wrapper, Node node) {
        return node.network() == wrapper && data.contains(node.address());
    }

    private static Seq<Map<String, NetworkVertex>> removeVertex(NetworkVertex vertex) {
        ArrayBuffer<NetworkVertex> seeds = new ArrayBuffer<>();
        Iterator<NetworkEdge> edges = vertex.edges.iterator();
        while (edges.hasNext()) {
            NetworkEdge edge = edges.next();
            NetworkVertex neighbor = edge.other(vertex);
            neighbor.edges.$minus$eq(edge);
            seeds.addOne(neighbor);
        }
        return NetworkGraphAlgorithms.searchGraphs(seeds);
    }

    private static Seq<Map<String, NetworkVertex>> removeEdge(NetworkEdge edge) {
        edge.left.edges.$minus$eq(edge);
        edge.right.edges.$minus$eq(edge);
        ArrayBuffer<NetworkVertex> seeds = new ArrayBuffer<>();
        seeds.addOne(edge.left);
        seeds.addOne(edge.right);
        return NetworkGraphAlgorithms.searchGraphs(seeds);
    }
}
