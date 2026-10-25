package li.cil.oc.common.block.traits;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public interface Tickable extends EntityBlock {
    BlockEntityType<? extends BlockEntity> getBlockEntityType();

    @Override
    default <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != getBlockEntityType()) return null;
        return (world, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof li.cil.oc.common.blockentity.traits.Tickable tickable) tickable.tick();
        };
    }
}
