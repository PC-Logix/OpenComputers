package li.cil.oc.integration.computercraft;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;

public final class ModComputerCraft implements ModProxy {
    public static final ModComputerCraft INSTANCE = new ModComputerCraft();

    private ModComputerCraft() {}

    @Override
    public Mod getMod() { return Mods.ComputerCraft(); }

    @Override
    public void preInitialize() {
        PeripheralProvider.register();
    }

    @Override
    public void initialize() {
        Driver.add(DriverComputerCraftMedia.INSTANCE);
        Driver.add(new DriverPeripheral());
        Driver.add(new ConverterLuaObject());
    }
}
