package li.cil.oc.common.capabilities;

import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.Direction;

public final class CapabilitySidedEnvironment {
    private CapabilitySidedEnvironment() {
    }

    public static class DefaultImpl implements SidedEnvironment {
        @Override
        public Node sidedNode(Direction side) {
            return null;
        }

        @Override
        public boolean canConnect(Direction side) {
            return false;
        }
    }
}
