package li.cil.oc.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RedstoneAware extends SimpleBlock {
    protected RedstoneAware(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.traits.RedstoneAware redstone) {
            return redstone.isOutputEnabled();
        }
        return false;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        return getSignal(state, world, pos, side);
    }

    @Deprecated
    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.traits.RedstoneAware redstone && side != null) {
            return Math.max(redstone.getOutput(side.getOpposite()), 0);
        }
        return super.getSignal(state, world, pos, side);
    }

    @Deprecated
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.traits.RedstoneAware redstone) {
            redstone.checkRedstoneInputChanged();
        }
    }
}
