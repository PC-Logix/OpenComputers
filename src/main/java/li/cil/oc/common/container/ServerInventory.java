package li.cil.oc.common.container;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.Driver;
import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.util.ItemUtils;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.collection.immutable.List;

import java.util.Objects;

public interface ServerInventory extends ItemStackInventory, MenuProvider {
    int rackSlot();

    @Override
    default DataComponentType<List<ImmutableItemStack>> component() {
        return OCComponentsJava.COMPONENTS.get();
    }

    @Override
    default void loadData(DataComponentHolder holder) {
        List<ImmutableItemStack> components = holder.get(OCComponentsJava.COMPONENTS.get());
        List<ImmutableItemStack> contents = holder.get(OCComponentsJava.CONTENTS.get());

        boolean hasComponent = false;
        if (components != null) {
            scala.collection.Iterator<ImmutableItemStack> iterator = components.iterator();
            while (iterator.hasNext()) {
                if (!iterator.next().isEmpty()) {
                    hasComponent = true;
                    break;
                }
            }
        }
        if (hasComponent) loadFrom(components);
        else if (contents != null) loadFrom(contents);
        else if (components != null) loadFrom(components);
    }

    @Override
    default void saveData(MutableDataComponentHolder holder) {
        ItemStackInventory.super.saveData(holder);
        holder.remove(OCComponentsJava.CONTENTS.get());
    }

    default int tier() {
        return Math.max(ItemUtils.caseTier(container()), 0);
    }

    @Override
    default int getContainerSize() {
        return InventorySlots.server[tier()].length;
    }

    @Override
    default String inventoryName() {
        return "server";
    }

    @Override
    default Component getDisplayName() {
        return getName();
    }

    @Override
    default int getMaxStackSize() {
        return 1;
    }

    @Override
    default boolean stillValid(Player player) {
        return false;
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        var driver = Driver.driverFor(stack, li.cil.oc.api.internal.Server.class);
        if (driver == null) return false;
        InventorySlots.InventorySlot provided = InventorySlots.server[tier()][slot];
        return Objects.equals(driver.slot(stack), provided.slot()) && driver.tier(stack) <= provided.tier();
    }

    @Override
    default net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.menu.Server(id, playerInventory, container(), this, tier(), rackSlot());
    }
}
