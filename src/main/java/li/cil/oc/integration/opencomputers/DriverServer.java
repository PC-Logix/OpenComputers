package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.server.component.Server;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverServer implements Item, HostAware {
    public static final DriverServer INSTANCE = new DriverServer();
    private DriverServer() {}
    @Override public boolean worksWith(ItemStack stack) {
        var items = new li.cil.oc.api.detail.ItemInfo[] {
                Items.get(Constants.ItemName$.MODULE$.ServerTier1()), Items.get(Constants.ItemName$.MODULE$.ServerTier2()),
                Items.get(Constants.ItemName$.MODULE$.ServerTier3()), Items.get(Constants.ItemName$.MODULE$.ServerTier4()),
                Items.get(Constants.ItemName$.MODULE$.ServerCreative())
        };
        var item = Items.get(stack);
        for (var info : items) if (info != null && Objects.equals(item, info)) return true;
        return false;
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (!(host instanceof Rack rack)) return null;
        int index = 0;
        while (index < rack.getContainerSize() && !rack.getItem(index).equals(stack)) index++;
        return new Server(rack, index);
    }
    @Override public String slot(ItemStack stack) { return Slot.RackMountable; }
}
