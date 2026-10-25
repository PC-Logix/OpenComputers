package li.cil.oc.integration.neoforge;

import li.cil.oc.api.Driver;
import li.cil.oc.api.IMC;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;

public final class ModNeoForge implements ModProxy {
    public static final ModNeoForge INSTANCE = new ModNeoForge();

    private ModNeoForge() {}

    @Override
    public Mod getMod() { return Mods.NeoForge(); }

    @Override
    public void initialize() {
        IMC.registerItemCharge("MinecraftForge",
                "li.cil.oc.integration.neoforge.EventHandlerNeoForge.canCharge",
                "li.cil.oc.integration.neoforge.EventHandlerNeoForge.charge");
        Driver.add(DriverEnergyStorage.INSTANCE);
    }
}
