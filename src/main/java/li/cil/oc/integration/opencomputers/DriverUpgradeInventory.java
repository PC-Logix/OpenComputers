package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.driver.item.Inventory;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class DriverUpgradeInventory implements Item, Inventory, HostAware {
    public static final DriverUpgradeInventory INSTANCE = new DriverUpgradeInventory();
    private DriverUpgradeInventory() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.InventoryUpgrade());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) { return null; }
    @Override public String slot(ItemStack stack) { return Slot.Upgrade; }
    @Override public int inventoryCapacity(ItemStack stack) { return 16; }
}
