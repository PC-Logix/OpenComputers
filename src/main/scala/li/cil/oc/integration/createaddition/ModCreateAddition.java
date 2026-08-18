package li.cil.oc.integration.createaddition;

import li.cil.oc.api.Driver;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;

public final class ModCreateAddition implements ModProxy {
    public static final ModCreateAddition INSTANCE = new ModCreateAddition();

    private ModCreateAddition() {
    }

    @Override
    public li.cil.oc.integration.Mod getMod() {
        return Mods.CreateAddition();
    }

    @Override
    public void initialize() {
        Driver.add(DriverAccumulator.Provider.INSTANCE);
        Driver.add(DriverAccumulator.INSTANCE);
    }
}
