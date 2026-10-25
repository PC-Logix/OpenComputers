package li.cil.oc.common.recipe;

import li.cil.oc.util.ItemColorizer;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class DecolorizeRecipe extends CustomRecipe {
    private final Item targetItem;

    public DecolorizeRecipe(ResourceLocation id, ItemLike target) {
        super(CraftingBookCategory.MISC);
        this.targetItem = target.asItem();
    }

    public Item targetItem() {
        return targetItem;
    }

    @Override
    public boolean matches(CraftingInput crafting, Level level) {
        int targetCount = 0;
        int waterCount = 0;
        for (int i = 0; i < crafting.size(); i++) {
            ItemStack stack = crafting.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == targetItem) targetCount++;
            else if (stack.getItem() == Items.WATER_BUCKET) waterCount++;
            else return false;
        }
        return targetCount == 1 && waterCount == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput crafting, HolderLookup.Provider provider) {
        ItemStack targetStack = ItemStack.EMPTY;
        for (int i = 0; i < crafting.size(); i++) {
            ItemStack stack = crafting.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == targetItem) {
                targetStack = stack.copy();
                targetStack.setCount(1);
            } else if (stack.getItem() != Items.WATER_BUCKET) {
                return ItemStack.EMPTY;
            }
        }

        if (targetStack.isEmpty()) return ItemStack.EMPTY;
        ItemColorizer.removeColor(targetStack);
        return targetStack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.DECOLORIZE.getSerializer();
    }
}
