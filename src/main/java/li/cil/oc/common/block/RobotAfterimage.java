package li.cil.oc.common.block;

import li.cil.oc.common.block.traits.Tickable;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.blockentity.RobotProxy;
import li.cil.oc.common.blockentity.Robot;
import li.cil.oc.util.BlockPosHelper;
import li.cil.oc.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.ScheduledTick;
import java.util.Optional;

public class RobotAfterimage extends SimpleBlock implements Tickable {
    public RobotAfterimage(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                               final CollisionContext context) {
        final var movingRobot = findMovingRobot(level, pos);
        if (movingRobot.isDefined()) {
            final Robot robot = (Robot) movingRobot.get();
            final VoxelShape shape = robot.getBlockState().getShape(level, robot.getBlockPos(), context);
            final BlockPos delta;
            if (robot.moveFrom().isDefined()) {
                final BlockPos from = (BlockPos) robot.moveFrom().get();
                final BlockPos robotPos = robot.getBlockPos();
                delta = new BlockPos(robotPos.getX() - from.getX(), robotPos.getY() - from.getY(), robotPos.getZ() - from.getZ());
            } else {
                delta = BlockPos.ZERO;
            }
            return shape.move(delta.getX(), delta.getY(), delta.getZ());
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    public void onPlace(final BlockState state, final Level level, final BlockPos pos,
                        final BlockState previousState, final boolean moved) {
        super.onPlace(state, level, pos, previousState, moved);
        if (!level.isClientSide()) {
            final long delay = Math.max((int) (Settings.get().moveDelay() * 20), 1) - 1L;
            final long triggerTime = level.getGameTime() + delay;
            level.getBlockTicks().schedule(new ScheduledTick<>(this, pos, triggerTime, level.nextSubTickCount()));
        }
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public boolean onDestroyedByPlayer(final BlockState state, final Level level, final BlockPos pos,
                                      final Player player, final boolean willHarvest, final FluidState fluid) {
        final var movingRobot = findMovingRobot(level, pos);
        if (movingRobot.isDefined()) {
            final Robot robot = (Robot) movingRobot.get();
            if (robot.isAnimatingMove() && robot.moveFrom().contains(pos)) {
                return robot.proxy().getBlockState().getBlock()
                    .onDestroyedByPlayer(state, level, pos, player, false, fluid);
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                            final Player player, final BlockHitResult hit) {
        final var movingRobot = findMovingRobot(level, pos);
        if (movingRobot.isDefined()) {
            final Robot robot = (Robot) movingRobot.get();
            return level.getBlockState(robot.getBlockPos()).useWithoutItem(level, player, hit);
        }
        return level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())
            ? InteractionResult.sidedSuccess(level.isClientSide()) : InteractionResult.PASS;
    }

    public scala.Option<Robot> findMovingRobot(final BlockGetter level, final BlockPos pos) {
        return scala.Option$.MODULE$.apply(findMovingRobotOptional(level, pos).orElse(null));
    }

    public Optional<Robot> findMovingRobotOptional(final BlockGetter level, final BlockPos pos) {
        for (final Direction side : Direction.values()) {
            final BlockPos targetPos = BlockPosHelper.relative(pos, side);
            if (level instanceof Level world && !world.isLoaded(targetPos)) continue;
            final BlockEntity blockEntity = level.getBlockEntity(targetPos);
            if (blockEntity instanceof RobotProxy proxy && proxy.robot().moveFrom().contains(pos)) {
                return Optional.of(proxy.robot());
            }
        }
        return Optional.empty();
    }

    @Override
    public BlockEntityType<? extends BlockEntity> getBlockEntityType() {
        return BlockEntityTypes.ROBOT.get();
    }
}
