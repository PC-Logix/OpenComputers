package li.cil.oc.server.component;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedLevelJava;
import net.minecraft.core.Direction;
import scala.Option;

/** Finds the first network node exposed by a neighboring environment. */
final class DebugCardNodeLookup {
    private DebugCardNodeLookup() { }

    static Option<Node> findNode(EnvironmentHost host, BlockPosition position) {
        ExtendedLevelJava level = new ExtendedLevelJava(host.getEnvironmentLevel());
        if (!level.blockExists(position)) return Option.empty();
        Object blockEntity = level.getBlockEntity(position);
        if (blockEntity instanceof SidedEnvironment environment) {
            for (Direction direction : Direction.values()) {
                Node node = environment.sidedNode(direction);
                if (node != null) return Option.apply(node);
            }
            return Option.empty();
        }
        if (blockEntity instanceof Environment environment) return Option.apply(environment.node());
        return Option.empty();
    }
}
