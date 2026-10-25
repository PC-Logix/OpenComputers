package li.cil.oc.common.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface InventoryProxy extends Container {
    Container inventory();

    default int offset() {
        return 0;
    }

    @Override
    default boolean isEmpty() {
        return inventory().isEmpty();
    }

    @Override
    default int getContainerSize() {
        return inventory().getContainerSize();
    }

    @Override
    default int getMaxStackSize() {
        return inventory().getMaxStackSize();
    }

    @Override
    default boolean stillValid(Player player) {
        return inventory().stillValid(player);
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        int offsetSlot = slot + offset();
        return isValidSlot(offsetSlot) && inventory().canPlaceItem(offsetSlot, stack);
    }

    @Override
    default ItemStack getItem(int slot) {
        int offsetSlot = slot + offset();
        return isValidSlot(offsetSlot) ? inventory().getItem(offsetSlot) : ItemStack.EMPTY;
    }

    @Override
    default ItemStack removeItem(int slot, int amount) {
        int offsetSlot = slot + offset();
        return isValidSlot(offsetSlot) ? inventory().removeItem(offsetSlot, amount) : ItemStack.EMPTY;
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        int offsetSlot = slot + offset();
        return isValidSlot(offsetSlot) ? inventory().removeItemNoUpdate(offsetSlot) : ItemStack.EMPTY;
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        int offsetSlot = slot + offset();
        if (isValidSlot(offsetSlot)) {
            inventory().setItem(offsetSlot, stack);
        }
    }

    @Override
    default void setChanged() {
        inventory().setChanged();
    }

    @Override
    default void startOpen(Player player) {
        inventory().startOpen(player);
    }

    @Override
    default void stopOpen(Player player) {
        inventory().stopOpen(player);
    }

    @Override
    default void clearContent() {
        inventory().clearContent();
    }

    private boolean isValidSlot(int slot) {
        return slot >= offset() && slot < getContainerSize() + offset();
    }
}
