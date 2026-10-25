package li.cil.oc.integration.projectred;

import li.cil.oc.api.IMC;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.util.BlockPosition;
import mrtjp.projectred.api.ProjectRedAPI;
import net.minecraft.core.Direction;

public final class ModProjectRed implements ModProxy, BundledRedstone.RedstoneProvider {
    public static final ModProjectRed INSTANCE = new ModProjectRed();

    private ModProjectRed() {}

    @Override
    public Mod getMod() { return Mods.ProjectRedTransmission(); }

    @Override
    public void initialize() {
        IMC.registerWrenchTool("li.cil.oc.integration.projectred.EventHandlerProjectRed.useWrench");
        IMC.registerWrenchToolCheck("li.cil.oc.integration.projectred.EventHandlerProjectRed.isWrench");
        BundledRedstone.addProvider(this);
        BundledProviderProjectRed.INSTANCE.install();
    }

    @Override
    public int computeInput(BlockPosition pos, Direction side) { return 0; }

    @Override
    public int[] computeBundledInput(BlockPosition pos, Direction side) {
        byte[] input = ProjectRedAPI.transmissionAPI.getBundledInput(pos.worldOptional().orElseThrow(), pos.toBlockPos(), side);
        if (input == null) return null;
        int[] result = new int[input.length];
        for (int i = 0; i < input.length; i++) result[i] = input[i] & 0xFF;
        return result;
    }
}
