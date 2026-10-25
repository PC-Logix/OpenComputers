package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.server.component.DiskDriveMountable;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverDiskDriveMountable implements Item, HostAware {
    public static final DriverDiskDriveMountable INSTANCE = new DriverDiskDriveMountable();
    private DriverDiskDriveMountable() {}
    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.DiskDriveMountable());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (!(host instanceof Rack rack)) return null;
        int index = 0;
        while (index < rack.getContainerSize() && !rack.getItem(index).equals(stack)) index++;
        return new DiskDriveMountable(rack, index);
    }
    @Override public String slot(ItemStack stack) { return Slot.RackMountable; }
}
