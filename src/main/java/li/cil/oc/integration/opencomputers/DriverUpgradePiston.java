package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Drone;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.server.component.UpgradePiston;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverUpgradePiston implements Item, HostAware {
    public static final DriverUpgradePiston INSTANCE = new DriverUpgradePiston();
    public static final EnvironmentProvider PROVIDER = stack ->
            INSTANCE.worksWith(stack) ? UpgradePiston.class : null;

    private DriverUpgradePiston() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.PistonUpgrade());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()) return null;
        if (host instanceof Drone drone) return new UpgradePiston.Drone(drone);
        if (host instanceof Tablet tablet) return new UpgradePiston.Tablet(tablet);
        if (host instanceof Rotatable rotatable) return new UpgradePiston.Rotatable(rotatable);
        return null;
    }
    @Override public String slot(ItemStack stack) { return Slot.Upgrade; }
}
