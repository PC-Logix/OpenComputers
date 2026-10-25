package li.cil.oc.common.block;

import li.cil.oc.common.block.traits.Tickable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public final class PowerDistributor extends SimpleBlock implements Tickable {
    public PowerDistributor(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.PowerDistributor(pos, state);
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return li.cil.oc.common.blockentity.BlockEntityTypes.POWER_DISTRIBUTOR.get();
    }
}
