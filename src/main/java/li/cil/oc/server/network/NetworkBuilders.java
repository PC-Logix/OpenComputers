package li.cil.oc.server.network;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.util.SideTracker;
import scala.Option;

import java.util.function.BooleanSupplier;

/** Java implementations for the fluent server network node builders. */
public final class NetworkBuilders {
    private NetworkBuilders() {}

    public static class NodeBuilder implements li.cil.oc.api.detail.Builder.NodeBuilder {
        private final Environment host;
        private final Visibility reachability;
        private final BooleanSupplier isServer;

        public NodeBuilder(Environment host, Visibility reachability, BooleanSupplier isServer) {
            this.host = host;
            this.reachability = reachability;
            this.isServer = isServer;
        }

        @Override public ComponentBuilder withComponent(String name, Visibility visibility) {
            return new NetworkBuilders.ComponentBuilder(host, reachability, name, visibility, isServer);
        }

        @Override public ComponentBuilder withComponent(String name) {
            return withComponent(name, reachability);
        }

        @Override public ConnectorBuilder withConnector(double bufferSize) {
            return new NetworkBuilders.ConnectorBuilder(host, reachability, bufferSize, isServer);
        }

        @Override public ConnectorBuilder withConnector() { return withConnector(0); }

        @Override public li.cil.oc.api.network.Node create() {
            return isServer.getAsBoolean() ? new PlainNode(host, reachability) : null;
        }
    }

    public static class ComponentBuilder implements li.cil.oc.api.detail.Builder.ComponentBuilder {
        private final Environment host;
        private final Visibility reachability;
        private final String name;
        private final Visibility visibility;
        private final BooleanSupplier isServer;

        public ComponentBuilder(Environment host, Visibility reachability, String name, Visibility visibility, BooleanSupplier isServer) {
            this.host = host;
            this.reachability = reachability;
            this.name = name;
            this.visibility = visibility;
            this.isServer = isServer;
        }

        @Override public ComponentConnectorBuilder withConnector(double bufferSize) {
            return new NetworkBuilders.ComponentConnectorBuilder(host, reachability, name, visibility, bufferSize, isServer);
        }

        @Override public ComponentConnectorBuilder withConnector() { return withConnector(0); }

        @Override public li.cil.oc.api.network.Component create() {
            if (!isServer.getAsBoolean()) return null;
            ComponentNode node = new ComponentNode(host, reachability, name);
            node.setVisibility(visibility);
            return node;
        }
    }

    public static class ConnectorBuilder implements li.cil.oc.api.detail.Builder.ConnectorBuilder {
        private final Environment host;
        private final Visibility reachability;
        private final double bufferSize;
        private final BooleanSupplier isServer;

        public ConnectorBuilder(Environment host, Visibility reachability, double bufferSize, BooleanSupplier isServer) {
            this.host = host;
            this.reachability = reachability;
            this.bufferSize = bufferSize;
            this.isServer = isServer;
        }

        @Override public ComponentConnectorBuilder withComponent(String name, Visibility visibility) {
            return new NetworkBuilders.ComponentConnectorBuilder(host, reachability, name, visibility, bufferSize, isServer);
        }

        @Override public ComponentConnectorBuilder withComponent(String name) {
            return withComponent(name, reachability);
        }

        @Override public li.cil.oc.api.network.Connector create() {
            return isServer.getAsBoolean() ? new ConnectorNode(host, reachability, bufferSize) : null;
        }
    }

    public static class ComponentConnectorBuilder implements li.cil.oc.api.detail.Builder.ComponentConnectorBuilder {
        private final Environment host;
        private final Visibility reachability;
        private final String name;
        private final Visibility visibility;
        private final double bufferSize;
        private final BooleanSupplier isServer;

        public ComponentConnectorBuilder(Environment host, Visibility reachability, String name, Visibility visibility,
                                         double bufferSize, BooleanSupplier isServer) {
            this.host = host;
            this.reachability = reachability;
            this.name = name;
            this.visibility = visibility;
            this.bufferSize = bufferSize;
            this.isServer = isServer;
        }

        @Override public li.cil.oc.api.network.ComponentConnector create() {
            if (!isServer.getAsBoolean()) return null;
            ComponentConnectorNode node = new ComponentConnectorNode(host, reachability, name, bufferSize);
            node.setVisibility(visibility);
            return node;
        }
    }

    private abstract static class BaseNode implements Node, NodeVarargPart {
        private final Environment host;
        private final Visibility reachability;
        private String address;
        private li.cil.oc.api.network.Network network;

        BaseNode(Environment host, Visibility reachability) {
            this.host = host;
            this.reachability = reachability;
        }

        @Override public Environment host() { return host; }
        @Override public Visibility reachability() { return reachability; }
        @Override public String address() { return address; }
        @Override public void address_$eq(String value) { address = value; }
        @Override public li.cil.oc.api.network.Network network() { return network; }
        @Override public void network_$eq(li.cil.oc.api.network.Network value) { network = value; }

        String nodeString() { return "Node(" + address + ", " + host + ")"; }
    }

    private static final class PlainNode extends BaseNode {
        PlainNode(Environment host, Visibility reachability) { super(host, reachability); }
        @Override public String toString() { return nodeString(); }
    }

    private static final class ComponentNode extends BaseNode implements Component {
        private final String name;

        ComponentNode(Environment host, Visibility reachability, String name) {
            super(host, reachability);
            this.name = name;
        }

        @Override public String name() { return name; }
        @Override public String toString() { return nodeString() + "@" + name; }
    }

    private static class ConnectorNode extends BaseNode implements Connector {
        private double localBufferSize;
        private double localBuffer;
        private Option<Distributor> distributor = scala.Option$.MODULE$.empty();

        ConnectorNode(Environment host, Visibility reachability, double localBufferSize) {
            super(host, reachability);
            this.localBufferSize = localBufferSize;
        }

        @Override public double localBufferSize() { return localBufferSize; }
        @Override public void localBufferSize_$eq(double value) { localBufferSize = value; }
        @Override public double localBuffer() { return localBuffer; }
        @Override public void localBuffer_$eq(double value) { localBuffer = value; }
        @Override public Option<Distributor> distributor() { return distributor; }
        @Override public void distributor_$eq(Option<Distributor> value) { distributor = value; }
        @Override public String toString() { return nodeString(); }
    }

    private static final class ComponentConnectorNode extends ConnectorNode implements ComponentConnector {
        private final String name;

        ComponentConnectorNode(Environment host, Visibility reachability, String name, double localBufferSize) {
            super(host, reachability, localBufferSize);
            this.name = name;
        }

        @Override public String name() { return name; }
        @Override public String toString() { return nodeString() + "@" + name; }
    }
}