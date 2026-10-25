package li.cil.oc.common.recipe;

import li.cil.oc.util.Color;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class ColorizeRecipe extends CustomRecipe {
    private final Item targetItem;

    public ColorizeRecipe(ResourceLocation id, ItemLike target) {
        super(CraftingBookCategory.MISC);
        this.targetItem = target.asItem();
    }

    public Item targetItem() {
        return targetItem;
    }

    @Override
    public boolean matches(CraftingInput crafting, Level level) {
        int targetCount = 0;
        int otherCount = 0;
        for (int i = 0; i < crafting.size(); i++) {
            ItemStack stack = crafting.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == targetItem) targetCount++;
            else if (Color.isDye(stack)) otherCount++;
            else return false;
        }
        return targetCount == 1 && otherCount > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput crafting, HolderLookup.Provider provider) {
        ItemStack targetStack = ItemStack.EMPTY;
        int[] color = {0, 0, 0};
        int colorCount = 0;
        int maximum = 0;

        for (int i = 0; i < crafting.size(); i++) {
            ItemStack stack = crafting.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == targetItem) {
                targetStack = stack.copy();
                targetStack.setCount(1);
            } else {
                java.util.Optional<TagKey<net.minecraft.world.item.Item>> dye = Color.findDye(stack);
                if (dye.isEmpty()) return ItemStack.EMPTY;

                int itemColor = Color.byTag(dye.get()).getTextureDiffuseColor();
                int red = ARGB32.red(itemColor);
                int green = ARGB32.green(itemColor);
                int blue = ARGB32.blue(itemColor);
                maximum += Math.max(red, Math.max(green, blue));
                color[0] += red;
                color[1] += green;
                color[2] += blue;
                colorCount++;
            }
        }

        if (targetStack.isEmpty()) return ItemStack.EMPTY;

        if (ItemColorizer.hasColor(targetStack)) {
            int itemColor = ItemColorizer.getColor(targetStack);
            float red = (itemColor >> 16 & 255) / 255.0F;
            float green = (itemColor >> 8 & 255) / 255.0F;
            float blue = (itemColor & 255) / 255.0F;
            maximum = (int) (maximum + Math.max(red, Math.max(green, blue)) * 255.0F);
            color[0] = (int) (color[0] + red * 255.0F);
            color[1] = (int) (color[1] + green * 255.0F);
            color[2] = (int) (color[2] + blue * 255.0F);
            colorCount++;
        }

        int red = color[0] / colorCount;
        int green = color[1] / colorCount;
        int blue = color[2] / colorCount;
        float max = (float) maximum / colorCount;
        float div = Math.max(red, Math.max(green, blue));
        red = (int) (red * max / div);
        green = (int) (green * max / div);
        blue = (int) (blue * max / div);
        ItemColorizer.setColor(targetStack, (red << 16) | (green << 8) | blue);
        return targetStack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.COLORIZE.getSerializer();
    }
}
