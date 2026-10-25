package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

/** NBT serialization for block-entity inventories. */
public final class InventoryStateJava {
    private InventoryStateJava() { }

    public static boolean load(ItemStack[] items, CompoundTag nbt, HolderLookup.Provider provider,
                               String inventoryTag, BiConsumer<Integer, ItemStack> updateItem) {
        if (!nbt.contains(inventoryTag, Tag.TAG_LIST)) return false;

        ListTag saved = nbt.getList(inventoryTag, Tag.TAG_COMPOUND);
        for (int slot = 0; slot < items.length; slot++) updateItem.accept(slot, ItemStack.EMPTY);
        for (int index = 0; index < saved.size(); index++) {
            CompoundTag entry = saved.getCompound(index);
            int slot = entry.getInt("slot");
            if (slot >= 0 && slot < items.length) {
                updateItem.accept(slot, ItemStack.parseOptional(provider, entry.getCompound("stack")));
            }
        }
        return true;
    }

    public static void save(ItemStack[] items, CompoundTag nbt, HolderLookup.Provider provider, String inventoryTag) {
        ListTag saved = new ListTag();
        for (int slot = 0; slot < items.length; slot++) {
            ItemStack stack = items[slot];
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", slot);
            entry.put("stack", stack.save(provider));
            saved.add(entry);
        }
        nbt.put(inventoryTag, saved);
    }
}
