package li.cil.oc.integration.opencomputers;

import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.common.container.ServerInventory;
import li.cil.oc.common.container.ItemStackInventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public final class InventoryProviderServer implements InventoryProvider {
    public static final InventoryProviderServer INSTANCE = new InventoryProviderServer();
    private InventoryProviderServer() {}
    @Override public boolean worksWith(ItemStack stack, Player player) {
        return DriverServer.INSTANCE.worksWith(stack);
    }
    @Override public Container getInventory(ItemStack stack, Player player) {
        return new ServerInventory() {
            private final ItemStack[] items = ItemStackInventory.newItems(this);
            @Override public ItemStack[] items() { return items; }
            @Override public ItemStack container() { return stack; }
            @Override public int rackSlot() { return -1; }
            @Override public Component getDisplayName() { return Component.empty(); }
            { if (container() != null && !container().isEmpty()) reinitialize(); }
        };
    }
}
