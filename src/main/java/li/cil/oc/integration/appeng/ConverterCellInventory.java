package li.cil.oc.integration.appeng;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import appeng.me.cells.BasicCellInventory;
import li.cil.oc.api.driver.Converter;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Map;

/** Exposes the current AE2 storage-cell statistics under the legacy OC names. */
public final class ConverterCellInventory implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        final StorageCell cell;
        if (value instanceof BasicCellInventory) {
            cell = (BasicCellInventory) value;
        } else if (value instanceof ItemStack) {
            cell = StorageCells.getCellInventory((ItemStack) value, null);
        } else {
            return;
        }

        if (cell == null) {
            return;
        }

        if (cell instanceof BasicCellInventory) {
            final BasicCellInventory basic = (BasicCellInventory) cell;
            output.put("storedItemTypes", basic.getStoredItemTypes());
            output.put("storedItemCount", basic.getStoredItemCount());
            output.put("remainingItemCount", basic.getRemainingItemCount());
            output.put("remainingItemTypes", basic.getRemainingItemTypes());
            output.put("getTotalItemTypes", basic.getTotalItemTypes());
            output.put("totalBytes", basic.getTotalBytes());
            output.put("freeBytes", basic.getFreeBytes());
            output.put("usedBytes", basic.getUsedBytes());
            output.put("unusedItemCount", basic.getUnusedItemCount());
            output.put("canHoldNewItem", basic.canHoldNewItem());
            output.put("fuzzyMode", basic.getFuzzyMode().toString());
        }

        final KeyCounter available = new KeyCounter();
        cell.getAvailableStacks(available);
        final ArrayList<ItemStack> availableItems = new ArrayList<>();
        for (var entry : available) {
            if (entry.getKey() instanceof AEItemKey) {
                availableItems.add(((AEItemKey) entry.getKey()).toStack((int) Math.min(Integer.MAX_VALUE, entry.getLongValue())));
            }
        }
        output.put("getAvailableItems", availableItems);
    }
}
