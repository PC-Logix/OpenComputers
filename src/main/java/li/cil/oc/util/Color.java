package li.cil.oc.util;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class Color {
    private static final Map<DyeColor, Integer> RGB_VALUES = new EnumMap<>(DyeColor.class);
    private static final Map<String, DyeColor> BY_NAME = new HashMap<>();
    private static final Map<TagKey<Item>, DyeColor> BY_TAG = new HashMap<>();

    static {
        RGB_VALUES.put(DyeColor.BLACK, 0x444444);
        RGB_VALUES.put(DyeColor.RED, 0xB3312C);
        RGB_VALUES.put(DyeColor.GREEN, 0x339911);
        RGB_VALUES.put(DyeColor.BROWN, 0x51301A);
        RGB_VALUES.put(DyeColor.BLUE, 0x6666FF);
        RGB_VALUES.put(DyeColor.PURPLE, 0x7B2FBE);
        RGB_VALUES.put(DyeColor.CYAN, 0x66FFFF);
        RGB_VALUES.put(DyeColor.LIGHT_GRAY, 0xABABAB);
        RGB_VALUES.put(DyeColor.GRAY, 0x666666);
        RGB_VALUES.put(DyeColor.PINK, 0xD88198);
        RGB_VALUES.put(DyeColor.LIME, 0x66FF66);
        RGB_VALUES.put(DyeColor.YELLOW, 0xFFFF66);
        RGB_VALUES.put(DyeColor.LIGHT_BLUE, 0xAAAAFF);
        RGB_VALUES.put(DyeColor.MAGENTA, 0xC354CD);
        RGB_VALUES.put(DyeColor.ORANGE, 0xEB8844);
        RGB_VALUES.put(DyeColor.WHITE, 0xF0F0F0);

        for (DyeColor color : DyeColor.values()) {
            BY_NAME.put(color.getName(), color);
            BY_TAG.put(color.getTag(), color);
        }
    }

    private static final int[] TIER_RGB_VALUES = {
        rgbValues(DyeColor.LIGHT_GRAY), rgbValues(DyeColor.YELLOW), rgbValues(DyeColor.CYAN),
        0x9A7D7D, rgbValues(DyeColor.MAGENTA)
    };

    private Color() {
    }

    public static int rgbValues(DyeColor color) {
        Integer value = RGB_VALUES.get(color);
        if (value == null) throw new NoSuchElementException("Unknown dye color: " + color);
        return value;
    }

    public static DyeColor byName(String name) {
        DyeColor color = BY_NAME.get(name);
        if (color == null) throw new NoSuchElementException("Unknown dye color: " + name);
        return color;
    }

    public static DyeColor byTag(TagKey<Item> tag) {
        DyeColor color = BY_TAG.get(tag);
        if (color == null) throw new NoSuchElementException("Unknown dye tag: " + tag);
        return color;
    }

    public static int byTier(int tier) {
        return TIER_RGB_VALUES[Math.max(0, Math.min(tier, TIER_RGB_VALUES.length - 1))];
    }

    public static Optional<TagKey<Item>> findDye(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        for (DyeColor color : DyeColor.values()) {
            TagKey<Item> tag = color.getTag();
            if (stack.is(tag)) return Optional.of(tag);
        }
        return Optional.empty();
    }

    public static boolean isDye(ItemStack stack) {
        return findDye(stack).isPresent();
    }

    public static DyeColor dyeColor(ItemStack stack) {
        return findDye(stack).map(Color::byTag).orElse(DyeColor.MAGENTA);
    }
}
