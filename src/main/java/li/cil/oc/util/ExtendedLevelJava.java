package li.cil.oc.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

public final class ExtendedLevelJava extends ExtendedBlockAccessJava {
    private final Level level;

    public ExtendedLevelJava(Level level) {
        super(level);
        this.level = level;
    }

    public boolean blockExists(BlockPosition position) { return level.isLoaded(position.toBlockPos()); }
    public boolean breakBlock(BlockPosition position) { return breakBlock(position, true); }
    public boolean breakBlock(BlockPosition position, boolean drops) { return level.destroyBlock(position.toBlockPos(), drops); }
    public void destroyBlockInWorldPartially(int entityId, BlockPosition position, int progress) {
        level.destroyBlockProgress(entityId, position.toBlockPos(), progress);
    }

    public boolean extinguishFire(Player player, BlockPosition position, Direction side) {
        BlockPos pos = position.toBlockPos();
        if (level.getBlockState(pos).is(BlockTags.FIRE)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return true;
        }
        return false;
    }

    public float getBlockHardness(BlockPosition position) {
        BlockPos pos = position.toBlockPos();
        return level.getBlockState(pos).getDestroySpeed(level, pos);
    }

    public int getBlockHarvestLevel(BlockPosition position) {
        BlockState state = position.worldOptional().orElseThrow().getBlockState(position.toBlockPos());
        if (state.is(Tags.Blocks.NEEDS_NETHERITE_TOOL)) return 4;
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 1;
        return 0;
    }

    public String getBlockHarvestTool(BlockPosition position) {
        BlockState state = position.worldOptional().orElseThrow().getBlockState(position.toBlockPos());
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return "pickaxe";
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) return "axe";
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return "shovel";
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) return "hoe";
        return null;
    }

    public int computeRedstoneSignal(BlockPosition position, Direction side) {
        BlockPosition neighbor = position.offset(side);
        return Math.max(isBlockProvidingPowerTo(neighbor, side), getIndirectPowerLevelTo(neighbor, side));
    }

    public int isBlockProvidingPowerTo(BlockPosition position, Direction side) {
        return level.getDirectSignal(position.toBlockPos(), side);
    }

    public int getIndirectPowerLevelTo(BlockPosition position, Direction side) {
        return level.getSignal(position.toBlockPos(), side);
    }

    public void notifyBlockUpdate(BlockPos pos) {
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
    }

    public void notifyBlockUpdate(BlockPosition position) {
        BlockPos pos = position.toBlockPos();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
    }

    public void notifyBlockUpdate(BlockPosition position, BlockState oldState, BlockState newState) {
        notifyBlockUpdate(position, oldState, newState, 3);
    }

    public void notifyBlockUpdate(BlockPosition position, BlockState oldState, BlockState newState, int flags) {
        level.sendBlockUpdated(position.toBlockPos(), oldState, newState, flags);
    }

    public void notifyBlockOfNeighborChange(BlockPosition position, Block block) {
        level.neighborChanged(position.toBlockPos(), block, position.toBlockPos());
    }

    @Deprecated
    public void notifyBlocksOfNeighborChange(BlockPosition position, Block block, boolean updateObservers) {
        level.updateNeighborsAt(position.toBlockPos(), block);
    }

    public void notifyBlocksOfNeighborChange(BlockPosition position, Block block, Direction side) {
        level.updateNeighborsAtExceptFromFacing(position.toBlockPos(), block, side);
    }

    public void playAuxSFX(int id, BlockPosition position, int data) {
        level.levelEvent(id, position.toBlockPos(), data);
    }

    public boolean setBlock(BlockPosition position, Block block) {
        return level.setBlockAndUpdate(position.toBlockPos(), block.defaultBlockState());
    }

    @Deprecated
    public boolean setBlock(BlockPosition position, Block block, int metadata, int flag) {
        var states = block.getStateDefinition().getPossibleStates();
        BlockState state = metadata >= 0 && metadata < states.size() ? states.get(metadata) : block.defaultBlockState();
        return level.setBlock(position.toBlockPos(), state, flag);
    }

    public boolean setBlockToAir(BlockPosition position) {
        return level.setBlockAndUpdate(position.toBlockPos(), Blocks.AIR.defaultBlockState());
    }

    public boolean isLoaded(BlockPosition position) { return level.isLoaded(position.toBlockPos()); }
}
