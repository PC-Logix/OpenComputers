package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.server.component.EEPROM;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverEEPROM implements Item {
    public static final DriverEEPROM INSTANCE = new DriverEEPROM();
    public static final EnvironmentProvider PROVIDER = stack -> INSTANCE.worksWith(stack) ? EEPROM.class : null;
    private DriverEEPROM() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.EEPROM());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide() ? null : new EEPROM();
    }
    @Override public String slot(ItemStack stack) { return Slot.EEPROM; }
    @Override public int tier(ItemStack stack) { return Tier.One; }
}
