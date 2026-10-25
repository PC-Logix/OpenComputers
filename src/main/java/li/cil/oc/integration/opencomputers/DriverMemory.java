package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverMemory implements Item, li.cil.oc.api.driver.item.Memory, CallBudget {
    public static final DriverMemory INSTANCE = new DriverMemory();
    private DriverMemory() {}

    @Override public double amount(ItemStack stack) {
        if (!(stack.getItem() instanceof li.cil.oc.common.item.Memory memory)) return 0.0;
        var sizes = Settings.get().ramSizes();
        int tier = Math.max(0, Math.min(memory.tier(), sizes.length - 1));
        return sizes[tier];
    }
    @Override public boolean worksWith(ItemStack stack) {
        var items = new li.cil.oc.api.detail.ItemInfo[] {
                Items.get(Constants.ItemName$.MODULE$.RAMTier1()), Items.get(Constants.ItemName$.MODULE$.RAMTier2()),
                Items.get(Constants.ItemName$.MODULE$.RAMTier3()), Items.get(Constants.ItemName$.MODULE$.RAMTier4()),
                Items.get(Constants.ItemName$.MODULE$.RAMTier5()), Items.get(Constants.ItemName$.MODULE$.RAMTier6()),
                Items.get(Constants.ItemName$.MODULE$.RAMTier7()), Items.get(Constants.ItemName$.MODULE$.RAMTier8())
        };
        var item = Items.get(stack);
        for (var info : items) if (info != null && Objects.equals(item, info)) return true;
        return false;
    }
    @Override public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, li.cil.oc.api.network.EnvironmentHost host) {
        return new li.cil.oc.server.component.Memory(tier(stack));
    }
    @Override public String slot(ItemStack stack) { return Slot.Memory; }
    @Override public int tier(ItemStack stack) {
        return stack.getItem() instanceof li.cil.oc.common.item.Memory memory ? memory.tier() / 2 : Tier.One;
    }
    @Override public double getCallBudget(ItemStack stack) {
        int tier = Math.max(Tier.One, Math.min(tier(stack), Tier.Four));
        return Settings.get().callBudgets()[tier];
    }
}
