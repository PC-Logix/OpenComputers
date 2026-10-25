package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.Direction;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class HubNetworkJavaTest {
    @Test
    public void relaysToEveryConnectedSideExceptTheSource() {
        List<Direction> relayed = new ArrayList<>();
        Node[] nodes = new Node[Direction.values().length];
        for (Direction side : Direction.values()) {
            Direction captured = side;
            nodes[side.ordinal()] = node(null, (method, args) -> {
                if (method.equals("sendToReachable")) relayed.add(captured);
                return null;
            });
        }
        nodes[Direction.WEST.ordinal()] = null;
        SidedEnvironment host = (SidedEnvironment) Proxy.newProxyInstance(
            SidedEnvironment.class.getClassLoader(), new Class<?>[]{SidedEnvironment.class},
            (proxy, method, args) -> method.getName().equals("sidedNode")
                ? nodes[((Direction) args[0]).ordinal()] : null);

        HubNetworkJava.relayPacket(host, Optional.of(Direction.NORTH), packet());

        assertEquals(List.of(Direction.DOWN, Direction.UP, Direction.SOUTH, Direction.EAST), relayed);
    }

    @Test
    public void firstPlugInNetworkIsPrimary() {
        Network network = proxy(Network.class, (method, args) -> null);
        Node first = node(network, (method, args) -> null);
        Node second = node(network, (method, args) -> null);

        assertTrue(HubNetworkJava.isPrimary(first, new Node[]{first, second}));
        assertFalse(HubNetworkJava.isPrimary(second, new Node[]{first, second}));
    }


    @Test
    public void onlyExternalSinglePacketMessagesEnterTheQueue() {
        Node ownNode = node(null, (method, args) -> null);
        Node externalNode = node(null, (method, args) -> null);
        Packet packet = packet();
        List<Packet> accepted = new ArrayList<>();
        List<Optional<Direction>> sides = new ArrayList<>();
        java.util.function.BiConsumer<Optional<Direction>, Packet> enqueue =
            (side, value) -> { sides.add(side); accepted.add(value); };

        HubNetworkJava.onPlugMessageJava(new Node[]{ownNode}, Direction.EAST,
            message("network.message", externalNode, packet), enqueue);
        HubNetworkJava.onPlugMessageJava(new Node[]{ownNode}, Direction.EAST,
            message("other", externalNode, packet), enqueue);
        HubNetworkJava.onPlugMessageJava(new Node[]{ownNode}, Direction.EAST,
            message("network.message", externalNode, packet, packet), enqueue);
        HubNetworkJava.onPlugMessageJava(new Node[]{ownNode}, Direction.EAST,
            message("network.message", ownNode, packet), enqueue);

        assertEquals(List.of(packet), accepted);
        assertEquals(List.of(Optional.of(Direction.EAST)), sides);
    }
    private static Node node(Network network, Invocation invocation) {
        return proxy(Node.class, (method, args) -> {
            if (method.equals("network")) return network;
            return invocation.invoke(method, args);
        });
    }


    private static li.cil.oc.api.network.Message message(String name, Node source, Object... data) {
        return proxy(li.cil.oc.api.network.Message.class, (method, args) -> switch (method) {
            case "name" -> name;
            case "source" -> source;
            case "data" -> data;
            default -> null;
        });
    }
    private static Packet packet() {
        return proxy(Packet.class, (method, args) -> null);
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
            (proxy, method, args) -> {
                if (method.getName().equals("equals")) return proxy == args[0];
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                return invocation.invoke(method.getName(), args);
            }));
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args);
    }

    @Test
    public void connectedHubNeedsAnAddressedNodeInANetwork() {
        li.cil.oc.api.network.Network network = proxy(li.cil.oc.api.network.Network.class,
            (method, args) -> null);
        Node connected = connectionNode("node-address", network);

        assertFalse(HubNetworkJava.isConnected(new Node[]{null}));
        assertFalse(HubNetworkJava.isConnected(new Node[]{connectionNode(null, network)}));
        assertFalse(HubNetworkJava.isConnected(new Node[]{connectionNode("node-address", null)}));
        assertTrue(HubNetworkJava.isConnected(new Node[]{connected}));
    }

    private static Node connectionNode(String address, li.cil.oc.api.network.Network network) {
        return proxy(Node.class, (method, args) -> switch (method) {
            case "address" -> address;
            case "network" -> network;
            default -> null;
        });
    }
}
