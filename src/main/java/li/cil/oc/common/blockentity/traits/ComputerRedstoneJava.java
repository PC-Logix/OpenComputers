package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.Direction;

/** Computer-specific redstone event forwarding after trait callbacks run. */
public final class ComputerRedstoneJava {
    private ComputerRedstoneJava() { }

    public static void sendInputChanged(Computer computer, Direction localSide, int oldValue, int newValue, int color) {
        RedstoneChangedEventArgs localArgs = new RedstoneChangedEventArgs(localSide, oldValue, newValue, color);
        computer.node().sendToNeighbors("redstone.changed", localArgs);
    }
}
