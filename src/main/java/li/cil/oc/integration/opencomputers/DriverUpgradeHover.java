package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.UpgradeHover;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverUpgradeHover implements Item, HostAware {
    public static final DriverUpgradeHover INSTANCE = new DriverUpgradeHover();
    private DriverUpgradeHover() {}

    @Override public boolean worksWith(ItemStack stack) {
        var first = Items.get(Constants.ItemName$.MODULE$.HoverUpgradeTier1());
        var second = Items.get(Constants.ItemName$.MODULE$.HoverUpgradeTier2());
        var item = Items.get(stack);
        return (first != null && Objects.equals(item, first)) || (second != null && Objects.equals(item, second));
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) { return null; }
    @Override public String slot(ItemStack stack) { return Slot.Upgrade; }
    @Override public int tier(ItemStack stack) {
        return stack.getItem() instanceof UpgradeHover upgrade ? upgrade.tier() : Tier.One;
    }
}
