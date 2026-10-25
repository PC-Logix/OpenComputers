package li.cil.oc.common.block;

import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.block.traits.GUI;
import li.cil.oc.common.block.traits.Tickable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public final class TapeDrive extends SimpleBlock implements Tickable, GUI {
    public TapeDrive(Properties properties) { super(properties); registerDefaultState(defaultBlockState().setValue(PropertyRotatable.Facing, Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PropertyRotatable.Facing); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(PropertyRotatable.Facing, context.getHorizontalDirection()); }
    @Override public void openGui(ServerPlayer player, Level level, BlockPos pos) { if (level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.TapeDrive drive) li.cil.oc.common.menu.MenuTypes.openTapeDriveGui(player, drive); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new li.cil.oc.common.blockentity.TapeDrive(pos, state); }
    @Override public boolean localOnBlockActivated(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack held, Direction side, float x, float y, float z) {
        if (!(level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.TapeDrive drive)) return false;
        if (player.isCrouching()) { if (!level.isClientSide()) drive.interact(player, held); return true; }
        return super.localOnBlockActivated(level, pos, player, hand, held, side, x, y, z);
    }
    @Deprecated @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.TapeDrive drive) drive.onRedstone(level.hasNeighborSignal(pos));
        super.neighborChanged(state, level, pos, block, from, moving);
    }
    @Override public net.minecraft.world.level.block.entity.BlockEntityType<?> getBlockEntityType() { return li.cil.oc.common.blockentity.BlockEntityTypes.TAPE_DRIVE.get(); }
}
