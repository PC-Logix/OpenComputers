package li.cil.oc.server.network;

import li.cil.oc.api.network.Node;

// Mixed into each concrete node implementation to keep the vararg bridges
// local to those implementations.
public interface NodeVarargPart extends Node {
    default void sendToAddress(String target, String name, Object... data) {
        if (network() != null) {
            network().sendToAddress(this, target, name, data);
        }
    }

    default void sendToNeighbors(String name, Object... data) {
        if (network() != null) {
            network().sendToNeighbors(this, name, data);
        }
    }

    default void sendToReachable(String name, Object... data) {
        if (network() != null) {
            network().sendToReachable(this, name, data);
        }
    }

    default void sendToVisible(String name, Object... data) {
        if (network() != null) {
            network().sendToVisible(this, name, data);
        }
    }
}
