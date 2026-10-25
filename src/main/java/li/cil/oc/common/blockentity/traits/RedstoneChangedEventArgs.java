package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.Direction;

public record RedstoneChangedEventArgs(Direction side, int oldValue, int newValue, int color) {
    public RedstoneChangedEventArgs(Direction side, int oldValue, int newValue) {
        this(side, oldValue, newValue, -1);
    }
}
