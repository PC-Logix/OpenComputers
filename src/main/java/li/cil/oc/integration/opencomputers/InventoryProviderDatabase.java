package li.cil.oc.integration.opencomputers;

import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.common.container.DatabaseInventory;
import li.cil.oc.common.container.ItemStackInventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InventoryProviderDatabase implements InventoryProvider {
    public static final InventoryProviderDatabase INSTANCE = new InventoryProviderDatabase();
    private InventoryProviderDatabase() {}
    @Override public boolean worksWith(ItemStack stack, Player player) {
        return DriverUpgradeDatabase.INSTANCE.worksWith(stack);
    }
    @Override public Container getInventory(ItemStack stack, Player player) {
        return new DatabaseInventory() {
            private final ItemStack[] items = ItemStackInventory.newItems(this);
            @Override public ItemStack[] items() { return items; }
            @Override public ItemStack container() { return stack; }
            @Override public boolean stillValid(Player player) { return player == player; }
            { DatabaseInventory.initialize(this); }
        };
    }
}
