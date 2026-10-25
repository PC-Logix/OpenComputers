package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.container.DatabaseInventory;
import li.cil.oc.common.container.ItemStackInventory;
import li.cil.oc.common.item.UpgradeDatabase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverUpgradeDatabase implements Item, HostAware {
    public static final DriverUpgradeDatabase INSTANCE = new DriverUpgradeDatabase();
    public static final EnvironmentProvider PROVIDER = stack ->
            INSTANCE.worksWith(stack) ? li.cil.oc.server.component.UpgradeDatabase.class : null;
    private DriverUpgradeDatabase() {}

    @Override public boolean worksWith(ItemStack stack) {
        var items = new li.cil.oc.api.detail.ItemInfo[] {
                Items.get(Constants.ItemName$.MODULE$.DatabaseUpgradeTier1()),
                Items.get(Constants.ItemName$.MODULE$.DatabaseUpgradeTier2()),
                Items.get(Constants.ItemName$.MODULE$.DatabaseUpgradeTier3())
        };
        var item = Items.get(stack);
        for (var info : items) if (info != null && Objects.equals(item, info)) return true;
        return false;
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()) return null;
        var inventory = new DatabaseInventory() {
            private final ItemStack[] items = ItemStackInventory.newItems(this);
            @Override public ItemStack[] items() { return items; }
            @Override public ItemStack container() { return stack; }
            @Override public boolean stillValid(Player player) { return false; }
            { DatabaseInventory.initialize(this); }
        };
        return new li.cil.oc.server.component.UpgradeDatabase(inventory);
    }
    @Override public String slot(ItemStack stack) { return Slot.Upgrade; }
    @Override public int tier(ItemStack stack) {
        return stack.getItem() instanceof UpgradeDatabase database ? database.tier() : Tier.One;
    }
}
