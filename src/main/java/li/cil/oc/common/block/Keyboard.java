package li.cil.oc.common.block;

import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.Items;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.util.BlockPosHelper;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedEnumFacing;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.ScheduledTick;

import java.util.Optional;

public class Keyboard extends SimpleBlock {
    public record ScreenAdjacency(li.cil.oc.common.blockentity.Keyboard keyboard, Screen screen,
                                  BlockPos screenPosition, Direction side) {}

    public Keyboard(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    public Object ImmibisMicroblocks_TransformableBlockMarker() {
        return null;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Pitch, PropertyRotatable.Yaw);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                               final CollisionContext context) {
        final Direction pitch = state.getValue(PropertyRotatable.Pitch);
        final Direction yaw = state.getValue(PropertyRotatable.Yaw);
        final Direction forward = pitch == Direction.DOWN || pitch == Direction.UP ? pitch : yaw;
        final Direction up = pitch == Direction.DOWN || pitch == Direction.UP ? yaw : Direction.UP;
        final Direction side = ExtendedEnumFacing.getRotation(forward, up);
        final float[] sizes = {7f / 16f, 4f / 16f, 7f / 16f};
        final float x0 = -up.getStepX() * sizes[1] - side.getStepX() * sizes[2] - forward.getStepX() * sizes[0];
        final float x1 = up.getStepX() * sizes[1] + side.getStepX() * sizes[2] - forward.getStepX() * 0.5f;
        final float y0 = -up.getStepY() * sizes[1] - side.getStepY() * sizes[2] - forward.getStepY() * sizes[0];
        final float y1 = up.getStepY() * sizes[1] + side.getStepY() * sizes[2] - forward.getStepY() * 0.5f;
        final float z0 = -up.getStepZ() * sizes[1] - side.getStepZ() * sizes[2] - forward.getStepZ() * sizes[0];
        final float z1 = up.getStepZ() * sizes[1] + side.getStepZ() * sizes[2] - forward.getStepZ() * 0.5f;
        return Shapes.box(
            Math.min(0.5 + x0, 0.5 + x1), Math.min(0.5 + y0, 0.5 + y1), Math.min(0.5 + z0, 0.5 + z1),
            Math.max(0.5 + x0, 0.5 + x1), Math.max(0.5 + y0, 0.5 + y1), Math.max(0.5 + z0, 0.5 + z1));
    }

    @Override
    public li.cil.oc.common.blockentity.Keyboard newBlockEntity(final BlockPos pos, final BlockState state) {
        return new li.cil.oc.common.blockentity.Keyboard(pos, state);
    }

    @Override
    public void onPlace(final BlockState state, final Level level, final BlockPos pos,
                        final BlockState previousState, final boolean moved) {
        if (!level.isClientSide()) {
            level.getBlockTicks().schedule(new ScheduledTick<>(this, pos, 10, level.nextSubTickCount()));
        }
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof li.cil.oc.common.blockentity.Keyboard keyboard) Network.joinOrCreateNetwork(keyboard);
        level.getBlockTicks().schedule(new ScheduledTick<>(this, pos, 10, level.nextSubTickCount()));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction clickedFace = context.getClickedFace();
        final Direction pitch = clickedFace == Direction.DOWN || clickedFace == Direction.UP ? clickedFace : Direction.NORTH;
        final Direction yaw = clickedFace == Direction.DOWN || clickedFace == Direction.UP
            ? context.getHorizontalDirection() : clickedFace;
        return super.getStateForPlacement(context).setValue(PropertyRotatable.Pitch, pitch)
            .setValue(PropertyRotatable.Yaw, yaw);
    }

    @Override
    public boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final Direction pitch = state.getValue(PropertyRotatable.Pitch);
        final Direction side = pitch == Direction.UP || pitch == Direction.DOWN
            ? pitch : state.getValue(PropertyRotatable.Yaw);
        final BlockPos supportPos = BlockPosHelper.relative(pos, side.getOpposite());
        if (!level.getBlockState(supportPos).isFaceSturdy(level, supportPos, side)) return false;
        final BlockEntity support = level.getBlockEntity(supportPos);
        return !(support instanceof li.cil.oc.common.blockentity.Screen screen) || screen.facing() != side;
    }

    @Deprecated
    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block,
                               final BlockPos fromPos, final boolean moved) {
        if (!canSurvive(level.getBlockState(pos), level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            InventoryUtils.spawnStackInWorld(BlockPositionsJava.apply(pos, level),
                Items.get(Constants.BlockName$.MODULE$.Keyboard()).createItemStack(1),
                java.util.Optional.empty(), java.util.Optional.empty());
        }
    }

    @Override
    public boolean localOnBlockActivated(final Level level, final BlockPos pos, final Player player,
                                         final InteractionHand hand, final ItemStack heldItem, final Direction side,
                                         final float hitX, final float hitY, final float hitZ) {
        return adjacencyInfoJava(level, pos).map(info ->
            info.screen().rightClick(level, info.screenPosition(), player, hand, heldItem, info.side(), 0, 0, 0, true)
        ).orElse(false);
    }

    public scala.Option<scala.Tuple4<li.cil.oc.common.blockentity.Keyboard, Screen, BlockPos, Direction>>
    adjacencyInfo(final Level level, final BlockPos pos) {
        return adjacencyInfoJava(level, pos).<scala.Option<scala.Tuple4<li.cil.oc.common.blockentity.Keyboard, Screen, BlockPos, Direction>>>map(info ->
            scala.Option.apply(new scala.Tuple4<>(info.keyboard(), info.screen(), info.screenPosition(), info.side()))
        ).orElseGet(scala.Option$.MODULE$::empty);
    }

    public Optional<ScreenAdjacency> adjacencyInfoJava(final Level level, final BlockPos pos) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof li.cil.oc.common.blockentity.Keyboard keyboard)) return Optional.empty();

        final Direction facing = keyboard.facing();
        final Direction back = facing.getOpposite();
        BlockPos screenPos = BlockPosHelper.relative(pos, back);
        Block block = level.getBlockState(screenPos).getBlock();
        if (block instanceof Screen screen) {
            return Optional.of(new ScreenAdjacency(keyboard, screen, screenPos, back));
        }

        final Direction forward = facing == Direction.UP || facing == Direction.DOWN ? keyboard.yaw() : Direction.UP;
        screenPos = BlockPosHelper.relative(pos, forward);
        block = level.getBlockState(screenPos).getBlock();
        if (block instanceof Screen screen) {
            return Optional.of(new ScreenAdjacency(keyboard, screen, screenPos, forward));
        }

        if (facing != Direction.UP && facing != Direction.DOWN) {
            final Direction below = forward.getOpposite();
            screenPos = BlockPosHelper.relative(pos, below);
            block = level.getBlockState(screenPos).getBlock();
            if (block instanceof Screen screen) {
                return Optional.of(new ScreenAdjacency(keyboard, screen, screenPos, below));
            }
        }
        return Optional.empty();
    }

    @Override
    public Direction[] getValidRotations(final Level level, final BlockPos pos) {
        return null;
    }
}
