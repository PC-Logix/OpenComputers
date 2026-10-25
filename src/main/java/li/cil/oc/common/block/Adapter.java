package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.common.block.traits.Tickable;
import li.cil.oc.integration.util.Wrench;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public class Adapter extends SimpleBlock implements Tickable {
    public static final MapCodec<Adapter> CODEC = BlockBehaviour.simpleCodec(Adapter::new);

    public Adapter(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<Adapter> codec() {
        return CODEC;
    }

    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Adapter adapter) {
            MenuTypes.openAdapterGui(player, adapter);
        }
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.Adapter(pos, state);
    }

    @Deprecated
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Adapter adapter) {
            adapter.neighborChanged();
        }
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbor) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Adapter adapter) {
            Direction side;
            if (neighbor.equals(pos.below())) side = Direction.DOWN;
            else if (neighbor.equals(pos.above())) side = Direction.UP;
            else if (neighbor.equals(pos.north())) side = Direction.NORTH;
            else if (neighbor.equals(pos.south())) side = Direction.SOUTH;
            else if (neighbor.equals(pos.west())) side = Direction.WEST;
            else if (neighbor.equals(pos.east())) side = Direction.EAST;
            else throw new IllegalArgumentException("not a neighbor");
            adapter.neighborChanged(side);
        }
    }

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand,
                                         ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (Wrench.holdsApplicableWrench(player, pos)) {
            Direction sideToToggle = player.isCrouching() ? side.getOpposite() : side;
            if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Adapter adapter) {
                if (!world.isClientSide()) {
                    boolean oldValue = adapter.openSides()[sideToToggle.ordinal()];
                    adapter.setSideOpen(sideToToggle, !oldValue);
                }
                return true;
            }
            return false;
        }
        if (!player.isCrouching()) {
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide()) {
                openGui(serverPlayer, world, pos);
            }
            return true;
        }
        return super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ);
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return BlockEntityTypes.ADAPTER.get();
    }
}
