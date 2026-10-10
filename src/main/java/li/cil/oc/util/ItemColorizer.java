package li.cil.oc.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** @author asie, Vexatos */
public final class ItemColorizer {
    private ItemColorizer() {
    }

    public static boolean hasColor(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains("display") && tag.getCompound("display").contains("color");
    }

    public static int getColor(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (tag == null || !tag.contains("display")) return -1;
        CompoundTag display = tag.getCompound("display");
        return display.contains("color") ? display.getInt("color") : -1;
    }

    public static void removeColor(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (tag == null) return;
        CompoundTag display = tag.getCompound("display");
        if (display.contains("color")) display.remove("color");
        if (display.isEmpty()) tag.remove("display");
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag.isEmpty() ? new CompoundTag() : tag);
    }

    public static void setColor(ItemStack stack, int color) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            if (!data.contains("display")) data.put("display", new CompoundTag());
            data.getCompound("display").putInt("color", color);
        });
    }

    private static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }
}
