package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.UpgradeContainerUpgrade;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverContainerUpgrade implements Item, li.cil.oc.api.driver.item.Container {
    public static final DriverContainerUpgrade INSTANCE = new DriverContainerUpgrade();
    private DriverContainerUpgrade() {}
    @Override public boolean worksWith(ItemStack stack) {
        var items = new li.cil.oc.api.detail.ItemInfo[] {
                Items.get(Constants.ItemName$.MODULE$.UpgradeContainerTier1()),
                Items.get(Constants.ItemName$.MODULE$.UpgradeContainerTier2()),
                Items.get(Constants.ItemName$.MODULE$.UpgradeContainerTier3())
        };
        var item = Items.get(stack);
        for (var info : items) if (info != null && Objects.equals(item, info)) return true;
        return false;
    }
    @Override public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, li.cil.oc.api.network.EnvironmentHost host) { return null; }
    @Override public String slot(ItemStack stack) { return Slot.Container; }
    @Override public String providedSlot(ItemStack stack) { return Slot.Upgrade; }
    @Override public int providedTier(ItemStack stack) { return tier(stack); }
    @Override public int tier(ItemStack stack) {
        return stack.getItem() instanceof UpgradeContainerUpgrade container ? container.tier() : Tier.One;
    }
}
