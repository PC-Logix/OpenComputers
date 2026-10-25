package li.cil.oc.server.network;

import li.cil.oc.api.network.Packet;
import scala.Option;
import scala.Option$;

import java.util.WeakHashMap;
import java.util.Objects;

public final class DebugNetwork {
    private static final WeakHashMap<DebugNode, Boolean> CARDS = new WeakHashMap<>();

    public static void add(DebugNode card) {
        CARDS.put(card, Boolean.TRUE);
    }

    public static void remove(DebugNode card) {
        CARDS.remove(card);
    }

    public static Option<DebugNode> getEndpoint(String address) {
        for (DebugNode card : CARDS.keySet()) {
            if (Objects.equals(card.address(), address)) return Option$.MODULE$.apply(card);
        }
        return Option$.MODULE$.apply(null);
    }

    public interface DebugNode {
        String address();

        void receivePacket(Packet packet);
    }

    private DebugNetwork() {
    }
}
