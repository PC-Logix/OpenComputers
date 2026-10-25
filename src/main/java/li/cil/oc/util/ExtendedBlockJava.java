package li.cil.oc.util;

import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

public final class ExtendedBlockJava {
    private final Block block;

    public ExtendedBlockJava(Block block) {
        this.block = block;
    }

    @Deprecated
    public boolean isAir(BlockPosition position) {
        return position.worldOptional().orElseThrow().isEmptyBlock(position.toBlockPos());
    }

    @Deprecated
    public boolean isReplaceable(BlockPosition position) {
        return block.defaultBlockState().is(BlockTags.REPLACEABLE);
    }

    @Deprecated
    public float getBlockHardness(BlockPosition position) {
        var world = position.worldOptional().orElseThrow();
        var blockPos = position.toBlockPos();
        return world.getBlockState(blockPos).getDestroySpeed(world, blockPos);
    }

    @Deprecated
    public int getComparatorInputOverride(BlockPosition position, Direction side) {
        var world = position.worldOptional().orElseThrow();
        return world.getBlockState(position.toBlockPos()).getAnalogOutputSignal(world, position.toBlockPos());
    }
}
