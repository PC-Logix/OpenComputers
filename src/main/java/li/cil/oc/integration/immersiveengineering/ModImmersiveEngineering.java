package li.cil.oc.integration.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.metal.ConnectorBundledBlockEntity;
import blusunrize.immersiveengineering.common.register.IEBlockEntities;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.util.BlockPosition;
import net.minecraft.core.Direction;

public final class ModImmersiveEngineering implements ModProxy, BundledRedstone.RedstoneProvider {
    public static final ModImmersiveEngineering INSTANCE = new ModImmersiveEngineering();

    private ModImmersiveEngineering() {}

    @Override
    public Mod getMod() { return Mods.ImmersiveEngineering(); }

    @Override
    public void preInitialize() {
        ConnectorBundledBlockEntity.EXTRA_SOURCES.add(new BundledProviderImmersiveEngineering());
    }

    @Override
    public void initialize() {
        BundledRedstone.addProvider(this);
    }

    @Override
    public int computeInput(BlockPosition pos, Direction side) { return 0; }

    @Override
    public int[] computeBundledInput(BlockPosition pos, Direction side) {
        var tile = pos.worldOptional().orElseThrow().getBlockEntity(
                pos.toBlockPos().relative(side), IEBlockEntities.CONNECTOR_BUNDLED.get());
        if (tile.isEmpty()) return null;
        int[] result = new int[16];
        for (int i = 0; i < result.length; i++) result[i] = tile.get().getValue(i);
        return result;
    }
}
