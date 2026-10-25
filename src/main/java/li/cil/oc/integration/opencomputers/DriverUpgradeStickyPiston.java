package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Drone;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.server.component.UpgradeStickyPiston;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverUpgradeStickyPiston implements Item, HostAware {
    public static final DriverUpgradeStickyPiston INSTANCE = new DriverUpgradeStickyPiston();
    public static final EnvironmentProvider PROVIDER = stack ->
            INSTANCE.worksWith(stack) ? UpgradeStickyPiston.class : null;

    private DriverUpgradeStickyPiston() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.StickyPistonUpgrade());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()) return null;
        if (host instanceof Drone drone) return new UpgradeStickyPiston.Drone(drone);
        if (host instanceof Tablet tablet) return new UpgradeStickyPiston.Tablet(tablet);
        if (host instanceof Rotatable rotatable) return new UpgradeStickyPiston.Rotatable(rotatable);
        return null;
    }
    @Override public String slot(ItemStack stack) { return Slot.Upgrade; }
}
