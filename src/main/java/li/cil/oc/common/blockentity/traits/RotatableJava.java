package li.cil.oc.common.blockentity.traits;

import li.cil.oc.common.block.SimpleBlock;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.server.BlockSyncPacketSender;
import li.cil.oc.util.ExtendedEnumFacing;
import li.cil.oc.util.ExtendedLevelJava;
import li.cil.oc.util.RotationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.function.BiPredicate;

/** Rotation calculations and block-state updates shared by Java rotation hosts. */
public final class RotatableJava {
    private static final Direction[] PITCHES = {Direction.UP, Direction.NORTH, Direction.DOWN};
    private static final Direction[] YAWS = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
    private static final Direction[] VALID_FACINGS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private RotatableJava() {}

    public static void notifyRotationChanged(Rotatable host) {
        try { onRotationChanged(host, (BlockEntity) host, host.isServer()); }
        catch (java.io.IOException error) { RotatableJava.<RuntimeException>throwUnchecked(error); }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E { throw (E) error; }

    public static Direction pitch(BlockState state) {
        return state.hasProperty(PropertyRotatable.Pitch)
                ? state.getValue(PropertyRotatable.Pitch) : Direction.NORTH;
    }

    public static Direction yaw(BlockState state) {
        if (state.hasProperty(PropertyRotatable.Yaw)) return state.getValue(PropertyRotatable.Yaw);
        if (state.hasProperty(PropertyRotatable.Facing)) return state.getValue(PropertyRotatable.Facing);
        return Direction.SOUTH;
    }

    public static Direction[] validFacings() {
        return VALID_FACINGS.clone();
    }

    public static void onRotationChanged(Rotatable rotatable, BlockEntity blockEntity, boolean server) throws java.io.IOException {
        Level level = blockEntity.getLevel();
        BlockPos position = blockEntity.getBlockPos();
        if (server) {
            BlockSyncPacketSender.sendRotatable(rotatable);
        } else {
            new ExtendedLevelJava(level).notifyBlockUpdate(position);
        }
        level.updateNeighborsAt(position, blockEntity.getBlockState().getBlock());
    }

    public static Direction toLocal(Direction pitch, Direction yaw, Direction value) {
        return value == null ? null : RotationHelper.toLocal(pitch, yaw, value);
    }

    public static Direction toGlobal(Direction pitch, Direction yaw, Direction value) {
        return value == null ? null : RotationHelper.toGlobal(pitch, yaw, value);
    }

    public static boolean fromEntity(Entity entity, BiPredicate<Direction, Direction> setter) {
        return setter.test(PITCHES[Math.round(entity.getXRot() / 90.0f) + 1],
                YAWS[Math.round(entity.getYRot() / 360.0f * 4.0f) & 3]);
    }

    public static boolean fromEntity(Entity entity, Direction localYaw,
                                     BiPredicate<Direction, Direction> setter) {
        return setter.test(PITCHES[Math.round(entity.getXRot() / 90.0f) + 1], localYaw);
    }

    public static boolean fromPitchAndYaw(float localPitch, float localYaw,
                                          BiPredicate<Direction, Direction> setter) {
        return setter.test(PITCHES[Math.round(localPitch / 90.0f) + 1],
                YAWS[Math.round(localYaw / 360.0f * 4.0f) & 3]);
    }

    public static boolean fromFacing(Direction value, Direction currentYaw,
                                     BiPredicate<Direction, Direction> setter) {
        return value == Direction.DOWN || value == Direction.UP
                ? setter.test(value, currentYaw)
                : setter.test(Direction.NORTH, value);
    }

    public static Direction facing(Direction pitch, Direction yaw) {
        return pitch == Direction.DOWN || pitch == Direction.UP ? pitch : yaw;
    }

    public static boolean invert(Direction pitch, Direction yaw,
                                  BiPredicate<Direction, Direction> setter) {
        Direction newPitch = pitch == Direction.DOWN || pitch == Direction.UP
                ? pitch.getOpposite() : Direction.NORTH;
        return setter.test(newPitch, yaw.getOpposite());
    }

    public static boolean rotate(Level level, BlockPos pos, Direction axis, Direction pitch, Direction yaw,
                                 Direction facing, BiPredicate<Direction, Direction> setter) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SimpleBlock simple) {
            Direction[] valid = simple.getValidRotations(level, pos);
            if (valid != null && Arrays.asList(valid).contains(axis)) {
                Direction value = ExtendedEnumFacing.getRotation(facing, axis);
                Direction newPitch;
                Direction newYaw;
                if (value == Direction.UP || value == Direction.DOWN) {
                    if (value == pitch) {
                        newPitch = value;
                        newYaw = ExtendedEnumFacing.getRotation(yaw, axis);
                    } else {
                        newPitch = value;
                        newYaw = yaw;
                    }
                } else {
                    newPitch = Direction.NORTH;
                    newYaw = value;
                }
                return setter.test(newPitch, newYaw);
            }
        }
        if (axis == Direction.UP || axis == Direction.DOWN) {
            Rotation rotation = axis == Direction.DOWN ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
            BlockState updated = state.rotate(level, pos, rotation);
            return !updated.equals(state) && level.setBlockAndUpdate(pos, updated);
        }
        return false;
    }

    public static boolean trySetPitchYaw(Level level, BlockPos pos, Direction pitch, Direction yaw,
                                         Runnable updateTranslation) {
        Direction safePitch = contains(PITCHES, pitch) ? pitch : Direction.NORTH;
        Direction safeYaw = contains(VALID_FACINGS, yaw) ? yaw : VALID_FACINGS[0];
        BlockState oldState = level.getBlockState(pos);
        BlockState newState;
        if (oldState.hasProperty(PropertyRotatable.Pitch) && oldState.hasProperty(PropertyRotatable.Yaw)) {
            newState = oldState.setValue(PropertyRotatable.Pitch, safePitch)
                    .setValue(PropertyRotatable.Yaw, safeYaw);
        } else if (oldState.hasProperty(PropertyRotatable.Facing)) {
            newState = oldState.setValue(PropertyRotatable.Facing, safeYaw);
        } else {
            return false;
        }

        if (oldState.hashCode() == newState.hashCode()) return false;
        level.setBlockAndUpdate(pos, newState);
        updateTranslation.run();
        return true;
    }

    private static boolean contains(Direction[] directions, Direction value) {
        for (Direction direction : directions) if (direction == value) return true;
        return false;
    }
}

