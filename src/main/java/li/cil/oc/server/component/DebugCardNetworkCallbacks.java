package li.cil.oc.server.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.server.network.DebugNetwork;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ResultWrapper;
import scala.Option;
import scala.Tuple3;
import scala.runtime.BoxedUnit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

/** Network and persisted-link behavior exposed by the debug card. */
final class DebugCardNetworkCallbacks {
    private DebugCardNetworkCallbacks() { }

    static Object[] connectToBlock(DebugCard card, EnvironmentHost host,
                                   Option<DebugCardAccessContext> access, Arguments args) {
        DebugCardSupport.checkAccess(access);
        int x = args.checkInteger(0);
        int y = args.checkInteger(1);
        int z = args.checkInteger(2);
        Option<Node> other = DebugCardNodeLookup.findNode(host, BlockPositionsJava.apply(x, y, z));
        if (other.isDefined()) {
            Option<Node> previous = card.remoteNodeForJava();
            if (previous.isDefined()) card.node().disconnect(previous.get());
            card.setRemoteNodeForJava(other);
            card.setRemoteNodePositionForJava(x, y, z);
            card.node().connect(other.get());
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(BoxedUnit.UNIT, "no node found at this position");
    }

    static void receivePacket(DebugCard card, Packet packet) {
        Object[] payload = packet.data();
        Object[] signal = new Object[payload.length + 4];
        signal[0] = "debug_message";
        signal[1] = packet.source();
        signal[2] = packet.port();
        signal[3] = 0.0;
        System.arraycopy(payload, 0, signal, 4, payload.length);
        card.node().sendToReachable("computer.signal", signal);
    }

    static String address(DebugCard card) {
        Node node = card.node();
        return node != null ? node.address() : "debug";
    }

    static void onConnect(DebugCard card, Node node) {
        if (Objects.equals(node, card.node())) {
            DebugNetwork.add(card);
            Option<?> position = card.remoteNodePositionForJava();
            if (position.isDefined()) {
                Tuple3<?, ?, ?> coordinates = (Tuple3<?, ?, ?>) position.get();
                int x = ((Number) coordinates._1()).intValue();
                int y = ((Number) coordinates._2()).intValue();
                int z = ((Number) coordinates._3()).intValue();
                Option<Node> remote = DebugCardNodeLookup.findNode(card.environmentHostForJava(), BlockPositionsJava.apply(x, y, z));
                card.setRemoteNodeForJava(remote);
                if (remote.isDefined()) node.connect(remote.get());
                else card.clearRemoteNodePositionForJava();
            }
        }
    }

    static void onDisconnect(DebugCard card, Node node) {
        if (Objects.equals(node, card.node())) {
            DebugNetwork.remove(card);
            Option<Node> remote = card.remoteNodeForJava();
            if (remote.isDefined()) remote.get().disconnect(node);
        } else {
            Option<Node> remote = card.remoteNodeForJava();
            if (remote.isDefined() && Objects.equals(remote.get(), node)) {
                card.setRemoteNodeForJava(Option.empty());
                card.clearRemoteNodePositionForJava();
            }
        }
    }
}
