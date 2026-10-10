package li.cil.oc.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ExtendedAABB {
    private ExtendedAABB() {
    }

    public static AABB unitBounds() {
        return new AABB(0, 0, 0, 1, 1, 1);
    }

    public static AABB offset(AABB bounds, BlockPos pos) {
        return new AABB(
            bounds.minX + pos.getX(), bounds.minY + pos.getY(), bounds.minZ + pos.getZ(),
            bounds.maxX + pos.getX(), bounds.maxY + pos.getY(), bounds.maxZ + pos.getZ());
    }

    public static Vec3 minVec(AABB bounds) {
        return new Vec3(bounds.minX, bounds.minY, bounds.minZ);
    }

    public static Vec3 maxVec(AABB bounds) {
        return new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ);
    }

    public static int volume(AABB bounds) {
        int sx = (int) Math.round((bounds.maxX - bounds.minX) * 16);
        int sy = (int) Math.round((bounds.maxY - bounds.minY) * 16);
        int sz = (int) Math.round((bounds.maxZ - bounds.minZ) * 16);
        return sx * sy * sz;
    }

    public static int surface(AABB bounds) {
        int sx = (int) Math.round((bounds.maxX - bounds.minX) * 16);
        int sy = (int) Math.round((bounds.maxY - bounds.minY) * 16);
        int sz = (int) Math.round((bounds.maxZ - bounds.minZ) * 16);
        return sx * sy * 2 + sx * sz * 2 + sy * sz * 2;
    }

    public static AABB rotateTowards(AABB bounds, Direction facing) {
        int count = switch (facing) {
            case WEST -> 3;
            case NORTH -> 2;
            case EAST -> 1;
            default -> 0;
        };
        return rotateY(bounds, count);
    }

    public static AABB rotateY(AABB bounds, int count) {
        Vec3 min = new Vec3(bounds.minX - 0.5, bounds.minY - 0.5, bounds.minZ - 0.5);
        Vec3 max = new Vec3(bounds.maxX - 0.5, bounds.maxY - 0.5, bounds.maxZ - 0.5);
        float angle = count * (float) Math.PI * 0.5f;
        min = min.yRot(angle);
        max = max.yRot(angle);
        return new AABB(
            Math.round(Math.min(min.x + 0.5, max.x + 0.5) * 32) / 32f,
            Math.round(Math.min(min.y + 0.5, max.y + 0.5) * 32) / 32f,
            Math.round(Math.min(min.z + 0.5, max.z + 0.5) * 32) / 32f,
            Math.round(Math.max(min.x + 0.5, max.x + 0.5) * 32) / 32f,
            Math.round(Math.max(min.y + 0.5, max.y + 0.5) * 32) / 32f,
            Math.round(Math.max(min.z + 0.5, max.z + 0.5) * 32) / 32f);
    }
}
