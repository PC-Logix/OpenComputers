package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.server.component.Keyboard;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverKeyboard implements Item, HostAware {
    public static final DriverKeyboard INSTANCE = new DriverKeyboard();
    private DriverKeyboard() {}
    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.BlockName$.MODULE$.Keyboard());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return new Keyboard(host);
    }
    @Override public String slot(ItemStack stack) { return Slot.Upgrade; }
}
