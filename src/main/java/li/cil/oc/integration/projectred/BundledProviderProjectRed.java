package li.cil.oc.integration.projectred;

import li.cil.oc.common.blockentity.traits.BundledRedstoneAware;
import mrtjp.projectred.api.IBundledTileInteraction;
import mrtjp.projectred.api.ProjectRedAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class BundledProviderProjectRed implements IBundledTileInteraction {
    public static final BundledProviderProjectRed INSTANCE = new BundledProviderProjectRed();

    private BundledProviderProjectRed() {}

    public void install() {
        ProjectRedAPI.transmissionAPI.registerBundledTileInteraction(this);
    }

    @Override
    public boolean isValidInteractionFor(Level level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof BundledRedstoneAware;
    }

    @Override
    public boolean canConnectBundled(Level level, BlockPos pos, Direction side) {
        return ((BundledRedstoneAware) level.getBlockEntity(pos)).isOutputEnabled();
    }

    @Override
    public byte[] getBundledSignal(Level level, BlockPos pos, Direction side) {
        int[] output = ((BundledRedstoneAware) level.getBlockEntity(pos)).getBundledOutput(side);
        byte[] result = new byte[output.length];
        for (int i = 0; i < output.length; i++) result[i] = (byte) Math.min(Math.max(output[i], 0), 255);
        return result;
    }
}
