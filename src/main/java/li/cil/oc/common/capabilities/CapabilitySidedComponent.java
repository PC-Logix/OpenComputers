package li.cil.oc.common.capabilities;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedComponent;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.Direction;

public final class CapabilitySidedComponent {
    private CapabilitySidedComponent() {
    }

    public static class SidedEnvironmentAdapter implements SidedEnvironment {
        private final Environment env;

        public SidedEnvironmentAdapter(Environment env) {
            this.env = env;
        }

        public Environment env() {
            return env;
        }

        @Override
        public Node sidedNode(Direction side) {
            return canConnect(side) ? env.node() : null;
        }

        @Override
        public boolean canConnect(Direction side) {
            return ((SidedComponent) env).canConnectNode(side);
        }
    }
}
