package li.cil.oc.common.block;

import li.cil.oc.Settings;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class FlatScreen extends Screen {
    private static final VoxelShape NORTH_SHAPE = Shapes.box(0, 0, 15.0 / 16.0, 1, 1, 1);
    private static final VoxelShape EAST_SHAPE = Shapes.box(0, 0, 0, 1.0 / 16.0, 1, 1);
    private static final VoxelShape SOUTH_SHAPE = Shapes.box(0, 0, 0, 1, 1, 1.0 / 16.0);
    private static final VoxelShape WEST_SHAPE = Shapes.box(15.0 / 16.0, 0, 0, 1, 1, 1);
    private static final VoxelShape UP_SHAPE = Shapes.box(0, 0, 0, 1, 1.0 / 16.0, 1);
    private static final VoxelShape DOWN_SHAPE = Shapes.box(0, 15.0 / 16.0, 0, 1, 1, 1);
    private final boolean isBack;

    public FlatScreen(final BlockBehaviour.Properties properties, final int tier, final boolean isBack) {
        super(properties, tier);
        this.isBack = isBack;
    }

    public boolean isBack() {
        return isBack;
    }

    @Override
    public void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Pitch, PropertyRotatable.Yaw);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction clicked = context.getClickedFace();
        final Direction pitch = clicked == Direction.DOWN || clicked == Direction.UP ? clicked : Direction.NORTH;
        final Direction yaw = clicked == Direction.DOWN || clicked == Direction.UP
            ? context.getHorizontalDirection() : clicked;
        return super.getStateForPlacement(context).setValue(PropertyRotatable.Pitch, pitch)
            .setValue(PropertyRotatable.Yaw, yaw);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                               final CollisionContext context) {
        return shapeFor(mountFace(state));
    }

    @Override
    public void tooltipBody(final ItemStack stack, final TooltipContext context,
                            final List<Component> tooltip, final TooltipFlag flag) {
        final int tier = tier();
        final scala.Tuple2<Object, Object> resolution = Settings.get().screenResolutionsByTier()[tier];
        final int depth = li.cil.oc.util.PackedColor.Depth.bits(Settings.get().screenDepthsByTier()[tier]);
        Tooltip.add(tooltip, flag, "screen", ((Number) resolution._1()).intValue(),
            ((Number) resolution._2()).intValue(), depth);
    }

    private Direction mountFace(final BlockState state) {
        final Direction pitch = state.getValue(PropertyRotatable.Pitch);
        final Direction facing = pitch == Direction.DOWN || pitch == Direction.UP
            ? pitch : state.getValue(PropertyRotatable.Yaw);
        return isBack ? facing : facing.getOpposite();
    }

    private VoxelShape shapeFor(final Direction facing) {
        return switch (facing) {
            case NORTH -> NORTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case UP -> UP_SHAPE;
            case DOWN -> DOWN_SHAPE;
        };
    }
}
