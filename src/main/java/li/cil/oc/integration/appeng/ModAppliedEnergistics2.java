package li.cil.oc.integration.appeng;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.IMC;
import li.cil.oc.common.blockentity.traits.PowerAcceptorHooks;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;

public final class ModAppliedEnergistics2 implements ModProxy {
    public static final ModAppliedEnergistics2 INSTANCE = new ModAppliedEnergistics2();

    private ModAppliedEnergistics2() {
    }

    @Override
    public li.cil.oc.integration.Mod getMod() {
        return Mods.AppliedEnergistics2();
    }

    @Override
    public void initialize() {
        PowerAcceptorHooks.install(AppliedEnergistics2.INSTANCE);
        OpenComputers.proxy().modBus().addListener(EventHandlerAppliedEnergistics2::onRegisterCapabilities);

        Driver.add(DriverController.Provider.INSTANCE);
        Driver.add(DriverBlockInterface.Provider.INSTANCE);
        Driver.add(DriverPartInterface.Provider.INSTANCE);
        Driver.add(DriverImportBus.Provider.INSTANCE);
        Driver.add(DriverExportBus.Provider.INSTANCE);
        Driver.add(DriverController.INSTANCE);
        Driver.add(DriverBlockInterface.INSTANCE);
        Driver.add(DriverPartInterface.INSTANCE);
        Driver.add(DriverImportBus.INSTANCE);
        Driver.add(DriverExportBus.INSTANCE);
        Driver.add(new ConverterCellInventory());

        IMC.registerWrenchTool("li.cil.oc.integration.appeng.EventHandlerAppliedEnergistics2.useWrench");
        IMC.registerWrenchToolCheck("li.cil.oc.integration.appeng.EventHandlerAppliedEnergistics2.isWrench");
    }
}
