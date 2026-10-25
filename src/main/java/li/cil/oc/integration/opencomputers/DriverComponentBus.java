package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.ComponentBus;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverComponentBus implements Item, Processor {
    public static final DriverComponentBus INSTANCE = new DriverComponentBus();
    private DriverComponentBus() {}
    @Override public boolean worksWith(ItemStack stack) {
        var items = new li.cil.oc.api.detail.ItemInfo[] {
                Items.get(Constants.ItemName$.MODULE$.ComponentBusTier1()),
                Items.get(Constants.ItemName$.MODULE$.ComponentBusTier2()),
                Items.get(Constants.ItemName$.MODULE$.ComponentBusTier3()),
                Items.get(Constants.ItemName$.MODULE$.ComponentBusTier4()),
                Items.get(Constants.ItemName$.MODULE$.ComponentBusCreative())
        };
        var item = Items.get(stack);
        for (var info : items) if (info != null && Objects.equals(item, info)) return true;
        return false;
    }
    @Override public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, li.cil.oc.api.network.EnvironmentHost host) { return null; }
    @Override public String slot(ItemStack stack) { return Slot.ComponentBus; }
    @Override public int tier(ItemStack stack) {
        return stack.getItem() instanceof ComponentBus bus ? Math.min(bus.tier(), Tier.Four) : Tier.One;
    }
    @Override public int supportedComponents(ItemStack stack) {
        return stack.getItem() instanceof ComponentBus bus ? Settings.get().cpuComponentSupport()[bus.tier()] : Tier.One;
    }
    @Override public Class<? extends li.cil.oc.api.machine.Architecture> architecture(ItemStack stack) { return null; }
}
