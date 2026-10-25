package li.cil.oc.integration.mekanism;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;

public final class ModMekanism implements ModProxy {
    public static final ModMekanism INSTANCE = new ModMekanism();

    private ModMekanism() {
    }

    @Override
    public li.cil.oc.integration.Mod getMod() {
        return Mods.Mekanism();
    }

    @Override
    public void initialize() {
        Driver.add(ConverterChemicalStack.INSTANCE);
        OpenComputers.proxy().modBus().addListener(EventHandlerMekanism::onRegisterCapabilities);
    }
}
