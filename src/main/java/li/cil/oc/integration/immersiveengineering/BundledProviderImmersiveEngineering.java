package li.cil.oc.integration.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.metal.ConnectorBundledBlockEntity.IBundledProvider;
import li.cil.oc.common.blockentity.traits.BundledRedstoneAware;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class BundledProviderImmersiveEngineering implements IBundledProvider {
    @Override
    public byte[] getEmittedState(Level level, BlockPos pos, Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof BundledRedstoneAware tile)) return null;
        int[] output = tile.getBundledOutput(direction);
        byte[] result = new byte[output.length];
        for (int i = 0; i < output.length; i++) result[i] = (byte) Math.min(Math.max(output[i], 0), 255);
        return result;
    }
}
