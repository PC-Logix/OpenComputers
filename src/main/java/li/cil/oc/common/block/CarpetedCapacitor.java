package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.block.traits.Tickable;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class CarpetedCapacitor extends Capacitor implements Tickable {
    public static final MapCodec<CarpetedCapacitor> CODEC = BlockBehaviour.simpleCodec(CarpetedCapacitor::new);

    public CarpetedCapacitor(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Capacitor> codec() {
        return CODEC;
    }

    @Override
    public li.cil.oc.common.blockentity.CarpetedCapacitor newBlockEntity(final BlockPos pos, final BlockState state) {
        return new li.cil.oc.common.blockentity.CarpetedCapacitor(pos, state);
    }

    @Override
    public BlockEntityType<? extends net.minecraft.world.level.block.entity.BlockEntity> getBlockEntityType() {
        return BlockEntityTypes.CARPETED_CAPACITOR.get();
    }
}
