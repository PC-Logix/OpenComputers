package li.cil.oc.common.block;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class HoloScreen extends Screen {
    private static final VoxelShape FLOOR_SHAPE = Shapes.box(0, 0, 0, 1, 0.5, 1);
    private static final VoxelShape CEILING_SHAPE = Shapes.box(0, 0.5, 0, 1, 1, 1);

    public HoloScreen(final BlockBehaviour.Properties properties, final int tier) {
        super(properties, tier);
        registerDefaultState(defaultBlockState().setValue(PropertyRotatable.Mount, Direction.UP)
            .setValue(PropertyRotatable.Facing, Direction.NORTH));
    }

    @Override
    public void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Mount, PropertyRotatable.Facing);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction mount = context.getClickedFace() == Direction.DOWN ? Direction.DOWN : Direction.UP;
        return super.getStateForPlacement(context).setValue(PropertyRotatable.Mount, mount)
            .setValue(PropertyRotatable.Facing, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                               final CollisionContext context) {
        return state.getValue(PropertyRotatable.Mount) == Direction.DOWN ? CEILING_SHAPE : FLOOR_SHAPE;
    }

    @Override
    public li.cil.oc.common.blockentity.HoloScreen newBlockEntity(final BlockPos pos, final BlockState state) {
        return new li.cil.oc.common.blockentity.HoloScreen(pos, state, tier());
    }

    @Override
    public Direction[] getValidRotations(final Level level, final BlockPos pos) {
        return new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    }

    @Override
    public boolean localOnBlockActivated(final Level level, final BlockPos pos, final Player player,
                                         final InteractionHand hand, final ItemStack heldItem,
                                         final Direction side, final float hitX, final float hitY, final float hitZ) {
        if (Wrench.holdsApplicableWrench(player, pos)
            || Items.get(heldItem) == Items.get(Constants.ItemName$.MODULE$.Analyzer())) {
            return super.localOnBlockActivated(level, pos, player, hand, heldItem, side, hitX, hitY, hitZ);
        }

        if (isResizeModifierDown(player)) {
            if (!(level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.HoloScreen screen)) return false;
            final boolean changed = screen.resize(screen.resizeOperationForWorldSide(side));
            if (changed) level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            return true;
        }

        if (!(level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.HoloScreen screen)) return false;
        if (player instanceof ServerPlayer serverPlayer && !level.isClientSide()) {
            MenuTypes.openHoloScreenGui(serverPlayer, screen);
        }
        return true;
    }

    private boolean isResizeModifierDown(final Player player) {
        return player.isCrouching() || player.isShiftKeyDown();
    }

    @Override
    public void tooltipBody(final ItemStack stack, final TooltipContext context,
                            final List<Component> tooltip, final TooltipFlag flag) {
        final int tier = tier();
        final scala.Tuple2<Object, Object> resolution = li.cil.oc.Settings.get().screenResolutionsByTier()[tier];
        final int depth = li.cil.oc.util.PackedColor.Depth.bits(
            li.cil.oc.Settings.get().screenDepthsByTier()[tier]);
        Tooltip.add(tooltip, flag, "screen", ((Number) resolution._1()).intValue(),
            ((Number) resolution._2()).intValue(), depth);
        Tooltip.addExtended(tooltip, flag, "holoscreen");
    }
}
