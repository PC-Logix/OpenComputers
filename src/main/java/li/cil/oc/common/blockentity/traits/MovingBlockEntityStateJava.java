package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.UnaryOperator;

/** Off-world position and rotation state for block entities carried by contraptions. */
public final class MovingBlockEntityStateJava {
    private Level level;
    private Vec3 position;
    private UnaryOperator<Vec3> rotation;
    private BlockState state;

    public Level level() { return level; }
    public void setLevel(Level value) { level = value; }
    public Vec3 position() { return position; }
    public void setPosition(Vec3 value) { position = value; }
    public UnaryOperator<Vec3> rotation() { return rotation; }
    public void setRotation(UnaryOperator<Vec3> value) { rotation = value; }
    public BlockState state() { return state; }
    public void setState(BlockState value) { state = value; }

    public boolean isMoving() { return position != null; }

    public void begin(Level level, Vec3 position, UnaryOperator<Vec3> rotation, BlockState state) {
        this.level = level;
        this.position = position;
        this.rotation = rotation;
        this.state = state;
    }

    public void update(Vec3 position, UnaryOperator<Vec3> rotation, BlockState state) {
        this.position = position;
        this.rotation = rotation;
        this.state = state;
    }

    public BlockPos blockPos(BlockPos original) {
        return position == null ? original : BlockPos.containing(position);
    }

    public BlockState blockState(BlockState original) {
        return state == null ? original : state;
    }

    public Direction direction(Direction side) {
        if (rotation == null) return side;
        Vec3 vector = rotation.apply(new Vec3(side.getStepX(), side.getStepY(), side.getStepZ()));
        return Direction.getNearest(vector.x, vector.y, vector.z);
    }

    public BlockPos neighbor(BlockPos original, Direction side) {
        return blockPos(original).relative(direction(side));
    }

    public void end() {
        level = null;
        position = null;
        rotation = null;
        state = null;
    }
}

