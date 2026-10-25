package li.cil.oc.server.network;

import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import scala.Option;
import scala.collection.Iterable;
import scala.collection.Iterator;
import scala.collection.Seq;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.HashMap;
import scala.collection.mutable.Map;

import java.util.Objects;

/** Mutable graph storage and operations backing the public Network API. */
final class NetworkCore implements Distributor {
    private final Map<String, NetworkVertex> data;
    private final ArrayBuffer<Connector> connectors = new ArrayBuffer<>();
    private final NetworkWrapper wrapper;
    private double globalBuffer;
    private double globalBufferSize;

    NetworkCore() {
        this(new HashMap<>());
    }

    NetworkCore(Node node) {
        this();
        li.cil.oc.server.network.Node mutableNode = (li.cil.oc.server.network.Node) node;
        addNew(mutableNode);
        mutableNode.onConnect(mutableNode);
    }

    NetworkCore(Map<String, NetworkVertex> data) {
        this.data = Objects.requireNonNull(data);
        wrapper = new NetworkWrapper(this);
        Iterator<NetworkVertex> vertices = data.values().iterator();
        while (vertices.hasNext()) {
            NetworkVertex vertex = vertices.next();
            if (vertex.data instanceof Connector connector) addConnector(connector);
            ((li.cil.oc.server.network.Node) vertex.data).network_$eq(wrapper);
        }
    }

    NetworkWrapper wrapper() { return wrapper; }
    Map<String, NetworkVertex> graphData() { return data; }
    ArrayBuffer<Connector> graphConnectors() { return connectors; }

    @Override public double globalBuffer() { return globalBuffer; }
    @Override public void globalBuffer_$eq(double value) { globalBuffer = value; }
    @Override public double globalBufferSize() { return globalBufferSize; }
    @Override public void globalBufferSize_$eq(double value) { globalBufferSize = value; }

    void remap(li.cil.oc.server.network.Node node, String newAddress) {
        NetworkGraphMutations.remap(data, node, newAddress, this::addNew);
    }

    boolean connect(li.cil.oc.server.network.Node nodeA, li.cil.oc.server.network.Node nodeB) {
        return NetworkGraphMutations.connect(data, wrapper, nodeA, nodeB, this::add);
    }

    boolean disconnect(li.cil.oc.server.network.Node nodeA, li.cil.oc.server.network.Node nodeB) {
        return NetworkGraphMutations.disconnect(this, data, wrapper, nodeA, nodeB);
    }

    boolean remove(li.cil.oc.server.network.Node node) {
        return NetworkGraphMutations.remove(this, data, connectors, wrapper, node);
    }

    Node node(String address) { return NetworkGraphQueries.node(data, address); }
    Iterable<Node> nodes() { return NetworkGraphQueries.nodes(data); }
    Iterable<Node> reachableNodes(Node reference) { return NetworkGraphQueries.reachableNodes(data, reference); }
    Iterable<Node> reachingNodes(Node reference) { return NetworkGraphQueries.reachingNodes(data, reference); }
    Iterable<Node> neighbors(Node node) { return NetworkGraphQueries.neighbors(data, node); }

    void sendToAddress(Node source, String target, String name, Object... args) {
        Option<Iterable<Node>> targets = NetworkMessageRouting.addressTargets(data, wrapper, source, target);
        if (targets.isDefined()) send(source, targets.get(), name, args);
    }

    void sendToNeighbors(Node source, String name, Object... args) {
        send(source, NetworkMessageRouting.neighborTargets(data, wrapper, source), name, args);
    }

    void sendToReachable(Node source, String name, Object... args) {
        send(source, NetworkMessageRouting.reachableTargets(data, wrapper, source), name, args);
    }

    void sendToVisible(Node source, String name, Object... args) {
        send(source, NetworkMessageRouting.visibleTargets(data, wrapper, source), name, args);
    }

    private boolean contains(li.cil.oc.server.network.Node node) {
        return node.network() == wrapper && data.contains(node.address());
    }

    private NetworkVertex node(Node node) { return data.apply(node.address()); }

    private NetworkVertex addNew(li.cil.oc.server.network.Node node) {
        NetworkVertex vertex = new NetworkVertex(node);
        if (node.address() == null || data.contains(node.address())) node.address_$eq(java.util.UUID.randomUUID().toString());
        data.put(node.address(), vertex);
        if (node instanceof Connector connector) addConnector(connector);
        node.network_$eq(wrapper);
        return vertex;
    }

    private boolean add(NetworkVertex oldNode, li.cil.oc.server.network.Node addedNode) {
        return NetworkGraphMutations.add(this, data, connectors, wrapper, oldNode, addedNode,
                this::addNew, this::add);
    }

    void handleSplit(Seq<Map<String, NetworkVertex>> subGraphs) {
        NetworkGraphAlgorithms.handleSplit(this, data, connectors, subGraphs,
                subGraph -> new NetworkCore(subGraph));
    }

    private void send(Node source, Iterable<Node> targets, String name, Object... args) {
        Message message = new NetworkMessage(source, name, args);
        Iterator<Node> nodes = targets.iterator();
        while (nodes.hasNext()) nodes.next().host().onMessage(message);
    }

    @Override public void addConnector(Connector connector) {
        NetworkPowerAccounting.addConnector(this, connectors, wrapper, connector);
    }

    @Override public void removeConnector(Connector connector) {
        NetworkPowerAccounting.removeConnector(this, connectors, connector);
    }

    @Override public double changeBuffer(double delta) {
        return NetworkPowerAccounting.changeBuffer(this, connectors, delta);
    }
}
