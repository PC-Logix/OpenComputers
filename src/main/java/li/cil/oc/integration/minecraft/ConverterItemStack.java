package li.cil.oc.integration.minecraft;

import li.cil.oc.Settings;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.util.ItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ConverterItemStack implements Converter {
    public static final ConverterItemStack INSTANCE = new ConverterItemStack();

    private ConverterItemStack() {}

    public static Object getTagValue(CompoundTag tag, String key) {
        return switch (tag.getTagType(key)) {
            case Tag.TAG_INT -> tag.getInt(key);
            case Tag.TAG_STRING -> tag.getString(key);
            case Tag.TAG_BYTE -> tag.getByte(key);
            case Tag.TAG_COMPOUND -> tag.getCompound(key);
            case Tag.TAG_LIST -> tag.getList(key, Tag.TAG_STRING);
            default -> null;
        };
    }

    public static Object withTag(CompoundTag tag, String key, int tagId, Function<Object, Object> function) {
        if (!tag.contains(key, tagId)) return null;
        Object value = getTagValue(tag, key);
        return value == null ? null : function.apply(value);
    }

    public static Object withCompound(CompoundTag tag, String key, Function<CompoundTag, Object> function) {
        return withTag(tag, key, Tag.TAG_COMPOUND, value -> function.apply((CompoundTag) value));
    }

    public static Object withList(CompoundTag tag, String key, Function<ListTag, Object> function) {
        return withTag(tag, key, Tag.TAG_STRING, value -> function.apply((ListTag) value));
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (!(value instanceof ItemStack stack)) return;

        if (Settings.get().insertIdsInConverters()) {
            output.put("id", Item.getId(stack.getItem()));
            output.put("oreNames", stack.getTags().map(key -> key.location().toString()).toArray(String[]::new));
        }

        CompoundTag tag = ItemUtils.getTag(stack);
        output.put("damage", stack.getDamageValue());
        output.put("maxDamage", stack.getMaxDamage());
        output.put("size", stack.getCount());
        output.put("maxSize", stack.getMaxStackSize());
        output.put("hasTag", tag != null);
        output.put("name", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        output.put("label", stack.getDisplayName().getString());

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            output.put("lore", lore.lines().stream().map(line -> line.getString()).collect(Collectors.joining("\n")));
        }

        IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (storage != null) output.put("Energy", storage.getEnergyStored());

        if (tag != null && Settings.get().allowItemStackNBTTags()) {
            output.put("tag", ItemUtils.saveTag(tag));
        }

        var enchantments = new ArrayList<Map<String, Object>>();
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            var enchantment = entry.getKey();
            int level = entry.getIntValue();
            Map<String, Object> details = new HashMap<>();
            details.put("name", enchantment.getKey());
            details.put("label", Enchantment.getFullname(enchantment, level));
            details.put("level", level);
            enchantments.add(details);
        }
        if (!enchantments.isEmpty()) output.put("enchantments", enchantments);
    }
}
