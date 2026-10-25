package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.Direction;

/** Shared direction transforms for block entities whose orientation may change. */
public interface RotationAware {
    default Direction toLocal(Direction value) {
        return value;
    }

    default Direction toGlobal(Direction value) {
        return value;
    }
}
