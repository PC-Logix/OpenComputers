package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.function.UnaryOperator;

/** Captured movement context shared by Java hosts and legacy adapters. */
public interface MovingBlockEntityHost {
    MovingBlockEntityStateJava movingContext();
    BlockPos getBlockPos();
    BlockState getBlockState();
    void initializeMovingHost();
    default boolean isMoving() { return movingContext().isMoving(); }
    default void beginMoving(Level level, Vec3 position, UnaryOperator<Vec3> rotation, BlockState state) {
        movingContext().begin(level, position, rotation, state);
        initializeMovingHost();
    }
    default void updateMovingPosition(Vec3 position, UnaryOperator<Vec3> rotation, BlockState state) { movingContext().update(position, rotation, state); }
    default BlockPos movingBlockPos() { return movingContext().blockPos(getBlockPos()); }
    default BlockState movingBlockState() { return movingContext().blockState(getBlockState()); }
    default Direction movingDirection(Direction side) { return movingContext().direction(side); }
    default BlockPos movingNeighbor(Direction side) { return movingContext().neighbor(getBlockPos(), side); }
    default void endMoving() { movingContext().end(); }
}
