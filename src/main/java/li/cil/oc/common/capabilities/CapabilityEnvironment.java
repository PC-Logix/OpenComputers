package li.cil.oc.common.capabilities;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;

public final class CapabilityEnvironment {
    private CapabilityEnvironment() {
    }

    public static class DefaultImpl implements Environment {
        private final Node node = Network.newNode(this, Visibility.None).create();

        @Override
        public Node node() {
            return node;
        }

        @Override
        public void onMessage(Message message) {
        }

        @Override
        public void onConnect(Node node) {
        }

        @Override
        public void onDisconnect(Node node) {
        }
    }
}
