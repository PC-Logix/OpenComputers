package li.cil.oc.integration.tfmg;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;

public final class ModTFMG implements ModProxy {
    public static final ModTFMG INSTANCE = new ModTFMG();

    private ModTFMG() {
    }

    @Override
    public li.cil.oc.integration.Mod getMod() {
        return Mods.TFMG();
    }

    @Override
    public void initialize() {
        Driver.add(DriverCableHub.Provider.INSTANCE);
        Driver.add(DriverCableHub.INSTANCE);
        Driver.add(DriverElectricSwitch.Provider.INSTANCE);
        Driver.add(DriverElectricSwitch.INSTANCE);
        Driver.add(DriverLargeElectricSwitch.Provider.INSTANCE);
        Driver.add(DriverLargeElectricSwitch.INSTANCE);
        Driver.add(DriverAccumulator.Provider.INSTANCE);
        Driver.add(DriverAccumulator.INSTANCE);
    }
}
