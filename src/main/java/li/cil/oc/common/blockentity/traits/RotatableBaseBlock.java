package li.cil.oc.common.blockentity.traits;

/** Rotation stored independently of the block state (robots, holograms, and prints). */
public interface RotatableBaseBlock extends Rotatable {
    RotatableBaseBlockJava rotationState();
    @Override default net.minecraft.core.Direction pitch() { return rotationState().pitch(); }
    @Override default net.minecraft.core.Direction yaw() { return rotationState().yaw(); }
    @Override default boolean trySetPitchYaw(net.minecraft.core.Direction pitch, net.minecraft.core.Direction yaw) {
        boolean changed = rotationState().setPitchYaw(pitch, yaw);
        if (changed) updateTranslation();
        return changed;
    }
}
