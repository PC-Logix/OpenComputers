package li.cil.oc.common.item;

import li.cil.oc.common.container.ServerInventory;
import li.cil.oc.common.container.ItemStackInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ServerInventoryHelper {
    private static final TooltipInventory TOOLTIP_INVENTORY = new TooltipInventory();

    private ServerInventoryHelper() { }

    public static ServerInventory tooltipInventory(ItemStack stack) {
        TOOLTIP_INVENTORY.container = stack;
        TOOLTIP_INVENTORY.reinitialize();
        return TOOLTIP_INVENTORY;
    }

    public static ServerInventory guiInventory(ItemStack stack, ServerPlayer serverPlayer) {
        ServerInventory inventory = new ServerInventory() {
            private final ItemStack[] items = ItemStackInventory.newItems(this);
            @Override public ItemStack[] items() { return items; }
            @Override public ItemStack container() { return stack; }
            @Override public int rackSlot() { return -1; }
            @Override public boolean stillValid(Player player) { return player == serverPlayer; }
            @Override public Component getDisplayName() { return getName(); }
        };
        inventory.reinitialize();
        return inventory;
    }

    private static final class TooltipInventory implements ServerInventory {
        private ItemStack container = ItemStack.EMPTY;
        private ItemStack[] items;

        @Override public ItemStack[] items() {
            if (items == null) items = ItemStackInventory.newItems(this);
            return items;
        }
        @Override public ItemStack container() { return container; }
        @Override public int rackSlot() { return -1; }
        @Override public Component getDisplayName() { return getName(); }
    }
}
