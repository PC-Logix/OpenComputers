package li.cil.oc.common.container;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface SimpleInventory extends Container, Nameable {
    @Override
    default boolean hasCustomName() {
        return false;
    }

    @Override
    default Component getDisplayName() {
        return getName();
    }

    @Override
    default int getMaxStackSize() {
        return 64;
    }

    // Items required in a slot before it's set to null (for ghost stacks).
    default int getInventoryStackRequired() {
        return 1;
    }

    @Override
    default void startOpen(Player player) {}

    @Override
    default void stopOpen(Player player) {}

    @Override
    default ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= getContainerSize()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = getItem(slot);
        if (stack.getCount() - amount < getInventoryStackRequired()) {
            setItem(slot, ItemStack.EMPTY);
        }
        else {
            stack = stack.split(amount);
            setChanged();
        }
        return stack.getCount() > 0 ? stack : ItemStack.EMPTY;
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= getContainerSize()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    default void clearContent() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            setItem(slot, ItemStack.EMPTY);
        }
    }
}
