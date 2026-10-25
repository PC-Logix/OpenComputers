package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.server.component.CapacitorMountable;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverCapacitorMountable implements Item, HostAware {
    public static final DriverCapacitorMountable INSTANCE = new DriverCapacitorMountable();
    private DriverCapacitorMountable() {}
    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.CapacitorMountable());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return host instanceof Rack rack ? new CapacitorMountable(rack) : null;
    }
    @Override public String slot(ItemStack stack) { return Slot.RackMountable; }
}
