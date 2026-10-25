package li.cil.oc.util;

import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ExtendedBlockAccessJava {
    protected final BlockGetter getter;

    public ExtendedBlockAccessJava(BlockGetter getter) {
        this.getter = getter;
    }

    public net.minecraft.world.level.block.Block getBlock(BlockPosition position) {
        return getter.getBlockState(position.toBlockPos()).getBlock();
    }

    public net.minecraft.world.level.material.MapColor getBlockMapColor(BlockPosition position) {
        return getBlockMetadata(position).getMapColor(getter, position.toBlockPos());
    }

    public net.minecraft.world.level.block.state.BlockState getBlockMetadata(BlockPosition position) {
        return getter.getBlockState(position.toBlockPos());
    }

    public BlockEntity getBlockEntity(BlockPosition position) {
        return getter.getBlockEntity(position.toBlockPos());
    }

    public BlockEntity getBlockEntity(EnvironmentHost host) {
        return getBlockEntity(BlockPositionsJava.apply(host));
    }

    public boolean isAirBlock(BlockPosition position) {
        return position.worldOptional().orElseThrow().isEmptyBlock(position.toBlockPos());
    }
}
