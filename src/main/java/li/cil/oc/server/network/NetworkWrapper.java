package li.cil.oc.server.network;

import li.cil.oc.api.network.Node;
import scala.jdk.javaapi.CollectionConverters;

/** Java adapter exposing an internal graph as the public network API. */
class NetworkWrapper implements li.cil.oc.api.network.Network, Distributor {
    private final NetworkCore network;

    NetworkWrapper(NetworkCore network) {
        this.network = network;
    }

    public NetworkCore network() {
        return network;
    }

    @Override
    public boolean connect(Node nodeA, Node nodeB) {
        return network.connect((li.cil.oc.server.network.Node) nodeA, (li.cil.oc.server.network.Node) nodeB);
    }

    @Override
    public boolean disconnect(Node nodeA, Node nodeB) {
        return network.disconnect((li.cil.oc.server.network.Node) nodeA, (li.cil.oc.server.network.Node) nodeB);
    }

    @Override
    public boolean remove(Node node) {
        return network.remove((li.cil.oc.server.network.Node) node);
    }

    @Override
    public Node node(String address) {
        return network.node(address);
    }

    @Override
    public java.lang.Iterable<Node> nodes() {
        return CollectionConverters.asJava(network.nodes());
    }

    @Override
    public java.lang.Iterable<Node> nodes(Node reference) {
        return CollectionConverters.asJava(network.reachableNodes(reference));
    }

    @Override
    public java.lang.Iterable<Node> neighbors(Node node) {
        return CollectionConverters.asJava(network.neighbors(node));
    }

    @Override
    public void sendToAddress(Node source, String target, String name, Object... data) {
        network.sendToAddress(source, target, name, data);
    }

    @Override
    public void sendToNeighbors(Node source, String name, Object... data) {
        network.sendToNeighbors(source, name, data);
    }

    @Override
    public void sendToReachable(Node source, String name, Object... data) {
        network.sendToReachable(source, name, data);
    }

    @Override
    public void sendToVisible(Node source, String name, Object... data) {
        network.sendToVisible(source, name, data);
    }

    @Override public double globalBuffer() { return network.globalBuffer(); }
    @Override public void globalBuffer_$eq(double value) { network.globalBuffer_$eq(value); }
    @Override public double globalBufferSize() { return network.globalBufferSize(); }
    @Override public void globalBufferSize_$eq(double value) { network.globalBufferSize_$eq(value); }
    @Override public void addConnector(Connector connector) { network.addConnector(connector); }
    @Override public void removeConnector(Connector connector) { network.removeConnector(connector); }
    @Override public double changeBuffer(double delta) { return network.changeBuffer(delta); }
}
