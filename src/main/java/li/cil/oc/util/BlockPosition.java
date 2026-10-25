package li.cil.oc.util;

import com.google.common.hash.Hashing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import scala.Option;
import scala.Tuple3;

import java.util.Optional;

public final class BlockPosition {
    private final int x;
    private final int y;
    private final int z;
    private final Option<Level> world;

    public BlockPosition(int x, int y, int z, Option<Level> world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }

    public BlockPosition(int x, int y, int z) {
        this(x, y, z, Option.empty());
    }

    public BlockPosition(double x, double y, double z, Option<Level> world) {
        this((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z), world);
    }

    public BlockPosition(double x, double y, double z) {
        this(x, y, z, Option.empty());
    }

    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public Option<Level> world() { return world; }
    public Optional<Level> worldOptional() {
        return world.isDefined() ? Optional.ofNullable(world.get()) : Optional.empty();
    }

    public BlockPosition offset(Direction direction, int n) {
        return new BlockPosition(x + direction.getStepX() * n, y + direction.getStepY() * n,
            z + direction.getStepZ() * n, world);
    }

    public BlockPosition offset(Direction direction) { return offset(direction, 1); }
    public Vec3 offset(double x, double y, double z) { return new Vec3(this.x + x, this.y + y, this.z + z); }
    public AABB bounds() { return new AABB(x, y, z, x + 1, y + 1, z + 1); }
    public BlockPos toBlockPos() { return new BlockPos(x, y, z); }
    public Vec3 toVec3() { return new Vec3(x + 0.5, y + 0.5, z + 0.5); }
    public Option<Tuple3<Integer, Integer, Integer>> unapply() { return Option.apply(new Tuple3<>(x, y, z)); }

    @Override
    public boolean equals(Object other) {
        return other instanceof BlockPosition position && position.x == x && position.y == y
            && position.z == z && position.world.equals(world);
    }

    @Override
    public int hashCode() {
        return Hashing.goodFastHash(32).newHasher(16).putInt(x).putInt(y).putInt(z)
            .putInt(world.hashCode()).hash().asInt();
    }
}
