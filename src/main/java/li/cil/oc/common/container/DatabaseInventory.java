package li.cil.oc.common.container;

import li.cil.oc.Settings;
import li.cil.oc.common.menu.Database;
import li.cil.oc.integration.opencomputers.DriverUpgradeDatabase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public interface DatabaseInventory extends ItemStackInventory, MenuProvider {
    static void initialize(DatabaseInventory inventory) {
        if (inventory.container() != null && !inventory.container().isEmpty()) inventory.reinitialize();
    }

    default int tier() {
        return DriverUpgradeDatabase.INSTANCE.tier(container());
    }

    @Override
    default int getContainerSize() {
        return Settings.get().databaseEntriesPerTier()[tier()];
    }

    @Override
    default String inventoryName() {
        return "database";
    }

    @Override
    default int getMaxStackSize() {
        return 1;
    }

    @Override
    default int getInventoryStackRequired() {
        return 1;
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return stack != container();
    }

    @Override
    default Component getDisplayName() {
        return Component.empty();
    }

    @Override
    default AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new Database(id, playerInventory, container(), this, tier());
    }
}
