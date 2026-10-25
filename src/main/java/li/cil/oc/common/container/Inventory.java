package li.cil.oc.common.container;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Settings;
import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.api.Persistable;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.StackOptionJava;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.collection.Iterable;
import scala.collection.Iterator;
import scala.collection.immutable.List;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;

/** Common item storage and persistence contract for container inventories. */
public interface Inventory extends SimpleInventory, Persistable {
    ItemStack[] items();

    default void updateItems(int slot, ItemStack stack) {
        items()[slot] = StackOptionJava.orEmpty(stack);
    }

    @Override
    default ItemStack getItem(int slot) {
        return slot >= 0 && slot < getContainerSize() && slot < items().length ? items()[slot] : ItemStack.EMPTY;
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= getContainerSize() || slot >= items().length) return;
        if (stack.isEmpty() && items()[slot].isEmpty()) return;
        if (items()[slot] == stack) return;

        ItemStack oldStack = items()[slot];
        updateItems(slot, ItemStack.EMPTY);
        if (!oldStack.isEmpty()) onItemRemoved(slot, oldStack);
        if (!stack.isEmpty() && stack.getCount() >= getInventoryStackRequired()) {
            if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
            updateItems(slot, stack);
        }
        if (!items()[slot].isEmpty()) onItemAdded(slot, items()[slot]);
        setChanged();
    }

    @Override
    default Component getName() {
        return Component.translatable(Settings.namespace() + "container." + inventoryName());
    }

    default String inventoryName() {
        return getClass().getSimpleName().toLowerCase();
    }

    @Override
    default boolean isEmpty() {
        for (ItemStack item : items()) if (!item.isEmpty()) return false;
        return true;
    }

    default void loadFrom(Iterable<ImmutableItemStack> value) {
        Iterator<ImmutableItemStack> saved = value.iterator();
        for (int slot = 0; slot < items().length; slot++) {
            items()[slot] = saved.hasNext() ? saved.next().mutableCopy() : ItemStack.EMPTY;
        }
    }

    default DataComponentType<List<ImmutableItemStack>> component() {
        return OCComponentsJava.CONTENTS.get();
    }

    @Override
    default void loadData(DataComponentHolder holder) {
        new ExtendedDataComponentHolder(holder).getComponentOptional(component()).ifPresent(this::loadFrom);
    }

    @Override
    default void saveData(MutableDataComponentHolder holder) {
        saveInventoryData(this, holder);
    }

    static void saveInventoryData(Inventory inventory, MutableDataComponentHolder holder) {
        java.util.List<ImmutableItemStack> stored = new ArrayList<>(inventory.items().length);
        for (ItemStack item : inventory.items()) stored.add(ImmutableItemStack.copyOf(item));
        holder.set(inventory.component(), CollectionConverters.asScala(stored).toList());
    }

    default void onItemAdded(int slot, ItemStack stack) { }

    default void onItemRemoved(int slot, ItemStack stack) { }
}
