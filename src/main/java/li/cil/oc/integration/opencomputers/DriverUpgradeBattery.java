package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.UpgradeBattery;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverUpgradeBattery implements Item, HostAware {
    public static final DriverUpgradeBattery INSTANCE = new DriverUpgradeBattery();
    private DriverUpgradeBattery() {}

    @Override public boolean worksWith(ItemStack stack) {
        var infos = new li.cil.oc.api.detail.ItemInfo[] {
                Items.get(Constants.ItemName$.MODULE$.BatteryUpgradeTier1()),
                Items.get(Constants.ItemName$.MODULE$.BatteryUpgradeTier2()),
                Items.get(Constants.ItemName$.MODULE$.BatteryUpgradeTier3())
        };
        var item = Items.get(stack);
        for (var info : infos) if (info != null && Objects.equals(item, info)) return true;
        return false;
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        return host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()
                ? null : new li.cil.oc.server.component.UpgradeBattery(tier(stack));
    }
    @Override public String slot(ItemStack stack) { return Slot.Upgrade; }
    @Override public int tier(ItemStack stack) {
        return stack.getItem() instanceof UpgradeBattery battery ? battery.tier() : Tier.One;
    }
}
