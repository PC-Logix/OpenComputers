package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.common.Slot;
import li.cil.oc.server.component.CreativeMemory;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverCreativeMemory implements Item, Memory, CallBudget {
    public static final DriverCreativeMemory INSTANCE = new DriverCreativeMemory();
    private DriverCreativeMemory() {}

    @Override public double amount(ItemStack stack) { return Double.POSITIVE_INFINITY; }
    @Override public double getCallBudget(ItemStack stack) { return 2.0; }
    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.RAMCreative());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, li.cil.oc.api.network.EnvironmentHost host) {
        return new CreativeMemory();
    }
    @Override public String slot(ItemStack stack) { return Slot.Memory; }
}
