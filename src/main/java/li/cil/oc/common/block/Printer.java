package li.cil.oc.common.block;

import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.common.block.traits.Tickable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Printer extends SimpleBlock implements Tickable {
    private final VoxelShape blockShape;

    public Printer(Properties properties) {
        super(properties);
        VoxelShape base = Block.box(0, 0, 0, 16, 8, 16);
        VoxelShape pillars = Shapes.or(Block.box(0, 8, 0, 3, 13, 3), Block.box(13, 8, 0, 16, 13, 3),
                Block.box(13, 8, 13, 16, 13, 16), Block.box(0, 8, 13, 3, 13, 16));
        VoxelShape ring = Shapes.join(Block.box(0, 13, 0, 16, 16, 16),
                Block.box(3, 13, 3, 13, 16, 13), BooleanOp.ONLY_FIRST);
        blockShape = Shapes.or(base, pillars, ring);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return blockShape;
    }

    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Printer printer) {
            MenuTypes.openPrinterGui(player, printer);
        }
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return null;
    }

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand,
                                         ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (!player.isCrouching()) {
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide()) {
                openGui(serverPlayer, world, pos);
            }
            return true;
        }
        return super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.api.util.StateAware stateful) {
            java.util.EnumSet<li.cil.oc.api.util.StateAware.State> currentState = stateful.getCurrentState();
            if (currentState.contains(li.cil.oc.api.util.StateAware.State.IsWorking)) return 15;
            if (currentState.contains(li.cil.oc.api.util.StateAware.State.CanWork)) return 10;
        }
        return 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.Printer(pos, state);
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return BlockEntityTypes.PRINTER.get();
    }
}
