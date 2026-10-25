package li.cil.oc.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class AudioCable extends net.minecraft.world.level.block.entity.BlockEntity {
    public AudioCable(BlockPos pos, BlockState state) { super(BlockEntityTypes.AUDIO_CABLE.get(), pos, state); }
}
