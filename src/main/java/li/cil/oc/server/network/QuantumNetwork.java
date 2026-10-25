package li.cil.oc.server.network;

import li.cil.oc.api.network.Packet;
import scala.collection.Iterable;
import scala.jdk.javaapi.CollectionConverters;

import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

public final class QuantumNetwork {
    private static final HashMap<String, WeakHashMap<QuantumNode, Boolean>> TUNNELS = new HashMap<>();

    public static void add(QuantumNode card) {
        TUNNELS.computeIfAbsent(card.tunnel(), ignored -> new WeakHashMap<>()).put(card, Boolean.TRUE);
    }

    public static void remove(QuantumNode card) {
        WeakHashMap<QuantumNode, Boolean> cards = TUNNELS.get(card.tunnel());
        if (cards != null) cards.remove(card);
    }

    public static Iterable<QuantumNode> getEndpoints(String tunnel) {
        WeakHashMap<QuantumNode, Boolean> cards = TUNNELS.get(tunnel);
        return CollectionConverters.asScala(cards == null ? List.<QuantumNode>of() : cards.keySet());
    }

    public interface QuantumNode {
        String tunnel();

        void receivePacket(Packet packet);
    }

    private QuantumNetwork() {
    }
}
