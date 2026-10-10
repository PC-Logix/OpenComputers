package li.cil.oc.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class Database extends AbstractMenu {
    private final ItemStack container;
    private final int tier;

    public Database(int id, Inventory playerInventory, ItemStack container, Container databaseInventory, int tier) {
        super(MenuTypes.DATABASE.get(), id, playerInventory, databaseInventory);
        this.container = container;
        this.tier = tier;

        int rows = (int) Math.ceil(Math.sqrt(databaseInventory.getContainerSize()));
        int offset = 8 + new int[]{3, 2, 0}[tier] * slotSize();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < rows; col++) {
                addSlotToContainer(offset + col * slotSize(), offset + row * slotSize(),
                    li.cil.oc.common.Slot.Any, li.cil.oc.common.Tier.Any);
            }
        }
        addPlayerInventorySlots(8, 174);
    }

    public ItemStack container() {
        return container;
    }

    public int tier() {
        return tier;
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == playerInventory().player;
    }

    @Override
    public void clicked(int slot, int dragType, ClickType clickType, Player player) {
        if (slot >= otherInventory().getContainerSize() || slot < 0) {
            super.clicked(slot, dragType, clickType, player);
            return;
        }
        Slot ghostSlot = slots.get(slot);
        if (ghostSlot != null) {
            ItemStack hand = getCarried();
            ghostSlot.set(hand.isEmpty() ? ItemStack.EMPTY : hand.copy());
        }
    }

    @Override
    public void tryTransferStackInSlot(Slot from, boolean intoPlayerInventory) {
        if (intoPlayerInventory) {
            from.setChanged();
            return;
        }
        ItemStack fromStack = from.getItem().copy();
        if (fromStack.isEmpty()) return;

        fromStack.setCount(1);
        for (int i = 0; i < slots.size(); i++) {
            Slot intoSlot = slots.get(i);
            if (intoSlot.container != from.container && !intoSlot.hasItem() && intoSlot.mayPlace(fromStack)
                && intoSlot.getMaxStackSize() > 0) {
                intoSlot.set(fromStack);
                return;
            }
        }
    }
}
