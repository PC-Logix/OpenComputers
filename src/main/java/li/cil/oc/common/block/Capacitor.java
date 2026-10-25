package li.cil.oc.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class Capacitor extends SimpleBlock {
    public static final MapCodec<Capacitor> CODEC = BlockBehaviour.simpleCodec(Capacitor::new);

    public Capacitor(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Capacitor> codec() {
        return CODEC;
    }

    @Deprecated
    @Override
    public boolean isRandomlyTicking(final BlockState state) {
        return true;
    }

    @Override
    public li.cil.oc.common.blockentity.Capacitor newBlockEntity(final BlockPos pos, final BlockState state) {
        return new li.cil.oc.common.blockentity.Capacitor(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Capacitor capacitor) {
            return (int) Math.round(15 * capacitor.node().localBuffer() / capacitor.node().localBufferSize());
        }
        return 0;
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        level.updateNeighborsAt(pos, this);
    }

    @Deprecated
    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                               final Block block, final BlockPos fromPos, final boolean moved) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Capacitor capacitor) {
            capacitor.recomputeCapacity(false);
        }
    }
}
