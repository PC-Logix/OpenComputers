package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.server.component.UpgradeCrafting;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverUpgradeCrafting implements Item, HostAware {
    public static final DriverUpgradeCrafting INSTANCE = new DriverUpgradeCrafting();
    public static final EnvironmentProvider PROVIDER = stack ->
            INSTANCE.worksWith(stack) ? UpgradeCrafting.class : null;
    private DriverUpgradeCrafting() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.CraftingUpgrade());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()) return null;
        return host instanceof Robot robot ? new UpgradeCrafting(robot) : null;
    }
    @Override public String slot(ItemStack stack) { return Slot.Upgrade; }
    @Override public int tier(ItemStack stack) { return Tier.Two; }
}
