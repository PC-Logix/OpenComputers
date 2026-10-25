package li.cil.oc.server.network;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

/** Mutable node API used by the server network implementation. */
public interface Node extends li.cil.oc.api.network.Node {
    Environment host();

    Visibility reachability();

    String address();

    void address_$eq(String value);

    Network network();

    void network_$eq(Network value);

    default boolean canBeReachedFrom(li.cil.oc.api.network.Node other) {
        return NodeJava.canBeReachedFrom(this, other);
    }

    default boolean isNeighborOf(li.cil.oc.api.network.Node other) {
        return NodeJava.isNeighborOf(this, other);
    }

    default Iterable<li.cil.oc.api.network.Node> reachableNodes() {
        return NodeJava.reachableNodes(this);
    }

    default Iterable<li.cil.oc.api.network.Node> neighbors() {
        return NodeJava.neighbors(this);
    }

    default void connect(li.cil.oc.api.network.Node other) {
        NodeJava.connect(this, other);
    }

    default void disconnect(li.cil.oc.api.network.Node other) {
        NodeJava.disconnect(this, other);
    }

    default void remove() {
        NodeJava.remove(this);
    }

    default void onConnect(li.cil.oc.api.network.Node other) {
        NodeJava.onConnect(this, other);
    }

    default void onDisconnect(li.cil.oc.api.network.Node other) {
        NodeJava.onDisconnect(this, other);
    }

    default void loadAddress(String newAddress) {
        NodeJava.loadAddress(this, newAddress);
    }

    @Override
    default void loadData(DataComponentHolder holder) {
        NodeJava.loadData(holder, this::loadAddress);
    }

    @Override
    default void saveData(MutableDataComponentHolder holder) {
        NodeJava.saveData(this, holder);
    }
}
