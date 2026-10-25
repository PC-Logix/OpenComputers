package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Rotation capability shared by ordinary and independently oriented block entities. */
public interface Rotatable extends RotationAware, li.cil.oc.api.internal.Rotatable {
    Level getLevel();
    BlockPos getBlockPos();
    BlockState getBlockState();
    boolean isServer();
    boolean isMoving();
    BlockState movingBlockState();

    default Direction pitch() { return RotatableJava.pitch(isMoving() ? movingBlockState() : getBlockState()); }
    default Direction yaw() { return RotatableJava.yaw(isMoving() ? movingBlockState() : getBlockState()); }
    default void pitch_$eq(Direction value) {
        trySetPitchYaw(value == Direction.DOWN || value == Direction.UP ? value : Direction.NORTH, yaw());
    }
    default void yaw_$eq(Direction value) {
        trySetPitchYaw(pitch(), value == Direction.DOWN || value == Direction.UP ? yaw() : value);
    }
    default boolean setFromEntityPitchAndYaw(Entity entity) { return RotatableJava.fromEntity(entity, this::trySetPitchYaw); }
    default boolean setFromEntityPitchAndYaw(Entity entity, Direction localYaw) {
        return RotatableJava.fromEntity(entity, localYaw, this::trySetPitchYaw);
    }
    default boolean setFromPitchAndYaw(float localPitch, float localYaw) {
        return RotatableJava.fromPitchAndYaw(localPitch, localYaw, this::trySetPitchYaw);
    }
    default boolean setFromPitchAndYaw(Direction newPitch, Direction newYaw) { return trySetPitchYaw(newPitch, newYaw); }
    default boolean setFromFacing(Direction value) { return RotatableJava.fromFacing(value, yaw(), this::trySetPitchYaw); }
    default boolean invertRotation() { return RotatableJava.invert(pitch(), yaw(), this::trySetPitchYaw); }
    @Override default Direction facing() { return RotatableJava.facing(pitch(), yaw()); }
    default boolean rotate(Direction axis) {
        return RotatableJava.rotate(getLevel(), getBlockPos(), axis, pitch(), yaw(), facing(), this::trySetPitchYaw);
    }
    @Override default Direction toLocal(Direction value) { return RotatableJava.toLocal(pitch(), yaw(), value); }
    @Override default Direction toGlobal(Direction value) { return RotatableJava.toGlobal(pitch(), yaw(), value); }
    default Direction[] validFacings() { return RotatableJava.validFacings(); }
    default void onRotationChanged() { RotatableJava.notifyRotationChanged(this); }
    default void updateTranslation() { if (getLevel() != null) onRotationChanged(); }
    default boolean trySetPitchYaw(Direction pitch, Direction yaw) {
        return RotatableJava.trySetPitchYaw(getLevel(), getBlockPos(), pitch, yaw, this::updateTranslation);
    }
}
