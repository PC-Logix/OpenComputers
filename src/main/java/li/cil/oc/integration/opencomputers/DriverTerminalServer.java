package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.component.RackKVM;
import li.cil.oc.common.component.TerminalServer;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverTerminalServer implements Item, HostAware {
    public static final DriverTerminalServer INSTANCE = new DriverTerminalServer();
    private DriverTerminalServer() {}
    @Override public boolean worksWith(ItemStack stack) {
        var terminal = Items.get(Constants.ItemName$.MODULE$.TerminalServer());
        var kvm = Items.get(Constants.ItemName$.MODULE$.RackKVM());
        var item = Items.get(stack);
        return (terminal != null && Objects.equals(item, terminal)) || (kvm != null && Objects.equals(item, kvm));
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (!(host instanceof Rack rack)) return null;
        int index = 0;
        while (index < rack.getContainerSize() && !rack.getItem(index).equals(stack)) index++;
        return Objects.equals(Items.get(stack), Items.get(Constants.ItemName$.MODULE$.RackKVM()))
                ? new RackKVM(rack, index) : new TerminalServer(rack, index);
    }
    @Override public String slot(ItemStack stack) { return Slot.RackMountable; }
}
