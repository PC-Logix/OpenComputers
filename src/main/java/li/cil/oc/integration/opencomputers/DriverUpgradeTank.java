package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.server.component.UpgradeTank;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverUpgradeTank implements Item, HostAware {
    public static final DriverUpgradeTank INSTANCE = new DriverUpgradeTank();
    private DriverUpgradeTank() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.TankUpgrade());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()
                ? null : new UpgradeTank(host, 16000);
    }
    @Override public String slot(ItemStack stack) { return Slot.Upgrade; }
}
