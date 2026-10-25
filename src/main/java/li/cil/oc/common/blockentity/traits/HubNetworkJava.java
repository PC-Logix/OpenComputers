package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.Direction;
import scala.Option;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/** Network-side routing helpers for Scala-composed hub hosts. */
public final class HubNetworkJava {
    private HubNetworkJava() { }

    public static void relayPacket(SidedEnvironment host, Option<Direction> sourceSide, Packet packet) {
        relayPacket(host, sourceSide.isDefined() ? Optional.of(sourceSide.get()) : Optional.empty(), packet);
    }

    public static void relayPacket(SidedEnvironment host, Optional<Direction> sourceSide, Packet packet) {
        for (Direction side : Direction.values()) {
            if (sourceSide.filter(value -> value == side).isPresent()) continue;
            Node node = host.sidedNode(side);
            if (node != null) node.sendToReachable("network.message", packet);
        }
    }


    public static void onPlugMessage(Node[] plugNodes, Direction plugSide, Message message,
                                     BiConsumer<Option<Direction>, Packet> enqueuePacket) {
        onPlugMessageJava(plugNodes, plugSide, message,
            (side, packet) -> enqueuePacket.accept(side.map(scala.Option::apply).orElseGet(scala.Option$.MODULE$::empty), packet));
    }

    public static void onPlugMessageJava(Node[] plugNodes, Direction plugSide, Message message,
                                         BiConsumer<Optional<Direction>, Packet> enqueuePacket) {
        if (!"network.message".equals(message.name())) return;
        for (Node node : plugNodes) {
            if (Objects.equals(node, message.source())) return;
        }
        Object[] data = message.data();
        if (data.length == 1 && data[0] instanceof Packet packet) {
            enqueuePacket.accept(Optional.of(plugSide), packet);
        }
    }

    public static boolean isConnected(Node[] plugNodes) {
        for (Node node : plugNodes) {
            if (node != null && node.address() != null && node.network() != null) return true;
        }
        return false;
    }
    public static boolean isPrimary(Node plugNode, Node[] plugNodes) {
        Object network = plugNode.network();
        for (Node node : plugNodes) {
            if (Objects.equals(node.network(), network)) return node == plugNode;
        }
        throw new ArrayIndexOutOfBoundsException(-1);
    }
}
