package li.cil.oc.util;

import dev.ryanhcode.sable.companion.SableCompanion;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * The coordinate boundary between OC's local block space and the physical
 * world space exposed by Sable/Aeronautics.
 *
 * Sable Companion supplies an identity implementation when Sable is not
 * present, so callers do not need optional-mod branches or Sable internals.
 */
public final class SableCompat {
    private SableCompat() {
    }

    public static Vec3 physicalPosition(Level level, Vec3 position) {
        if (level == null || position == null) return position;
        var projected = SableCompanion.INSTANCE.projectOutOfSubLevel(level,
            new Vector3d(position.x, position.y, position.z));
        return new Vec3(projected.x, projected.y, projected.z);
    }

    public static Vec3 physicalPosition(EnvironmentHost host) {
        return physicalPosition(host.getEnvironmentLevel(),
            new Vec3(host.xPosition(), host.yPosition(), host.zPosition()));
    }

    public static Vec3 physicalPosition(Level level, BlockPos position) {
        return physicalPosition(level, Vec3.atCenterOf(position));
    }

    /** Transform a local block-space direction into a world-space vector. */
    public static Vec3 physicalDirection(Level level, Vec3 position, Direction facing) {
        if (facing == null) return Vec3.ZERO;
        if (level == null || position == null) return Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 origin = physicalPosition(level, position);
        Vec3 target = physicalPosition(level,
            position.add(facing.getStepX(), facing.getStepY(), facing.getStepZ()));
        return target.subtract(origin);
    }

    /** Transform a world-space direction vector into a local-space vector. */
    public static Vec3 localDirection(Level level, Vec3 position, Vec3 facing) {
        if (facing == null) return Vec3.ZERO;
        if (level == null || position == null) return facing;
        var sublevel = SableCompanion.INSTANCE.getContaining(level, position);
        return sublevel == null ? facing : sublevel.logicalPose().transformNormalInverse(facing);
    }

    public static double distanceSquared(Level level, Vec3 first, Vec3 second) {
        if (level == null) return first.distanceToSqr(second);
        return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(level, first, second);
    }

    public static boolean isInPlotGrid(Level level, Vec3 position) {
        return level != null && position != null && SableCompanion.INSTANCE.isInPlotGrid(level, position);
    }

    public static Direction physicalFacing(Level level, Vec3 position, Direction facing) {
        if (facing == null) return null;
        Vec3 direction = physicalDirection(level, position, facing);
        return Direction.getNearest(direction.x, direction.y, direction.z);
    }

    /** Zero is north; heading increases clockwise toward east. */
    public static double physicalHeading(Level level, Vec3 position, Direction facing) {
        Vec3 direction = physicalDirection(level, position, facing);
        double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (horizontalLength < 1.0e-9) return 0.0;
        return positiveDegrees(Math.toDegrees(Math.atan2(direction.x, -direction.z)));
    }

    /** Zero is north; heading increases clockwise toward east. */
    public static double localHeading(Level level, Vec3 position, Vec3 facing, Vec3 side) {
        Vec3 direction = localDirection(level, position, facing);
        double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (horizontalLength < 1.0e-3) {
            direction = localDirection(level, position, side);
            horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
            if (horizontalLength < 1.0e-3) return 0.0;
            return positiveDegrees(Math.toDegrees(Math.atan2(direction.z, direction.x)));
        }
        return positiveDegrees(Math.toDegrees(Math.atan2(direction.x, -direction.z)));
    }

    public static double physicalPitch(Level level, Vec3 position, Direction facing) {
        Vec3 direction = physicalDirection(level, position, facing);
        return Math.toDegrees(Math.atan2(direction.y,
            Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
    }

    public static double localPitch(Level level, Vec3 position, Vec3 facing) {
        Vec3 direction = localDirection(level, position, facing);
        return Math.toDegrees(Math.atan2(direction.y,
            Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
    }

    /** Convert a physical cardinal direction into the local direction at a block. */
    public static Direction localFacing(Level level, Vec3 position, Direction facing) {
        if (level == null || position == null || facing == null) return facing;
        for (Direction candidate : Direction.values()) {
            if (physicalFacing(level, position, candidate) == facing) return candidate;
        }
        return facing;
    }

    private static double positiveDegrees(double degrees) {
        double heading = degrees % 360.0;
        return heading < 0.0 ? heading + 360.0 : heading;
    }
}
