package li.cil.oc.common.block;

import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.block.traits.Tickable;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.function.ToIntFunction;

public class Projector extends SimpleBlock implements Tickable {
    /** Block state is the authoritative, always-synced copy of the display mode. */
    public static final BooleanProperty ScreenMode = BooleanProperty.create("screen_mode");

    /** A small amount of block light while the projector is enabled. */
    public static final int LightLevel = 4;

    public static final ToIntFunction<BlockState> LightEmission = state ->
            state.getValue(BlockStateProperties.LIT) ? LightLevel : 0;

    private final VoxelShape shape = Shapes.box(0, 0, 0, 1, 0.75, 1);

    public Projector(BlockBehaviour.Properties props) {
        super(props.lightLevel(LightEmission));
        registerDefaultState(defaultBlockState()
                .setValue(PropertyRotatable.Facing, Direction.NORTH)
                .setValue(BlockStateProperties.LIT, true)
                .setValue(ScreenMode, false));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Facing, BlockStateProperties.LIT, ScreenMode);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(PropertyRotatable.Facing, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shape;
    }

    // The OC wrench handles SimpleBlocks through onItemUseFirst, before block
    // activation. Projectors use their facing property for the beam direction,
    // but a wrench hit is reserved for switching display modes.
    @Override
    public boolean rotateBlock(Level world, BlockPos pos, Direction axis) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof li.cil.oc.common.blockentity.Projector projector) {
            if (!world.isClientSide) projector.toggleMode();
            return true;
        }
        return false;
    }

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand,
                                         ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (Wrench.holdsApplicableWrench(player, pos)) {
            if (!world.isClientSide) {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof li.cil.oc.common.blockentity.Projector projector) {
                    projector.toggleMode();
                    Wrench.wrenchUsed(player, pos);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void tooltipBody(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Tooltip.add(tooltip, flag, "projector");
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.Projector(pos, state);
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return BlockEntityTypes.PROJECTOR.get();
    }
}
