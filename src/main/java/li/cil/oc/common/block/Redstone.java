package li.cil.oc.common.block;

import li.cil.oc.common.block.traits.Tickable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public final class Redstone extends RedstoneAware implements Tickable {
    public Redstone(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.Redstone(pos, state);
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return li.cil.oc.common.blockentity.BlockEntityTypes.REDSTONE_IO.get();
    }
}
