package li.cil.oc.integration.create;

import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;

public final class ModCreate implements ModProxy {
    public static final ModCreate INSTANCE = new ModCreate();

    private ModCreate() {}

    @Override
    public Mod getMod() { return Mods.Create(); }

    @Override
    public void initialize() {
        CreateContraptionTransformers.register();
        CreateDrivers.register();
        DisplaySource.BY_BLOCK_ENTITY.add(BlockEntityTypes.ADAPTER.get(), AllDisplaySources.COMPUTER.get());
    }
}
