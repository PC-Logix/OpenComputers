package li.cil.oc.util;

import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import scala.Option;
import scala.Tuple3;

/** Shared factory methods for constructing block positions. */
public final class BlockPositionsJava {
    private BlockPositionsJava() {}

    public static BlockPosition apply(int x, int y, int z, Level world) {
        return new BlockPosition(x, y, z, Option.apply(world));
    }

    public static BlockPosition apply(int x, int y, int z) {
        return new BlockPosition(x, y, z);
    }

    public static BlockPosition apply(double x, double y, double z, Level world) {
        return new BlockPosition(x, y, z, Option.apply(world));
    }

    public static BlockPosition apply(double x, double y, double z) {
        return new BlockPosition(x, y, z);
    }

    public static BlockPosition apply(Vec3 value) {
        return new BlockPosition(value.x, value.y, value.z);
    }

    public static BlockPosition apply(Vec3 value, Level world) {
        return new BlockPosition(value.x, value.y, value.z, Option.apply(world));
    }

    public static BlockPosition apply(EnvironmentHost host) {
        return apply(host.xPosition(), host.yPosition(), host.zPosition(), host.getEnvironmentLevel());
    }

    public static BlockPosition apply(Entity entity) {
        return apply(entity.getX(), entity.getY(), entity.getZ(), entity.level());
    }

    public static BlockPosition apply(BlockPos position, Level world) {
        return apply(position.getX(), position.getY(), position.getZ(), world);
    }

    public static BlockPosition apply(BlockPos position) {
        return apply(position.getX(), position.getY(), position.getZ());
    }

    public static Option<Tuple3<Integer, Integer, Integer>> unapply(BlockPos position) {
        return Option.apply(new Tuple3<>(position.getX(), position.getY(), position.getZ()));
    }
}
