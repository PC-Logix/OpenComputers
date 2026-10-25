package li.cil.oc.common.recipe;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.common.Loot;
import li.cil.oc.integration.util.Wrench;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import scala.collection.mutable.Buffer;

public class LootDiskCyclingRecipe implements CraftingRecipe {
    private final CraftingBookCategory bookCategory;

    public LootDiskCyclingRecipe(CraftingBookCategory bookCategory) {
        this.bookCategory = bookCategory;
    }

    @Override
    public CraftingBookCategory category() {
        return bookCategory;
    }

    @Override
    public boolean matches(CraftingInput crafting, Level level) {
        int stackCount = 0;
        boolean hasDisk = false;
        boolean hasWrench = false;
        for (ItemStack stack : collectStacks(crafting)) {
            stackCount++;
            hasDisk |= Loot.isLootDisk(stack);
            hasWrench |= Wrench.isWrench(stack);
        }
        return stackCount == 2 && hasDisk && hasWrench;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Recipes.LOOTDISK_CYCLING.getSerializer();
    }

    @Override
    public ItemStack assemble(CraftingInput crafting, HolderLookup.Provider provider) {
        Buffer<ItemStack> disks = Loot.disksForCycling();
        if (disks.isEmpty()) return ItemStack.EMPTY;

        for (ItemStack stack : collectStacks(crafting)) {
            if (!Loot.isLootDisk(stack)) continue;
            ResourceLocation currentFactory = getLootFactoryName(stack);
            int oldIndex = -1;
            for (int i = 0; i < disks.length(); i++) {
                if (java.util.Objects.equals(getLootFactoryName(disks.apply(i)), currentFactory)) {
                    oldIndex = i;
                    break;
                }
            }
            return disks.apply((oldIndex + 1) % disks.length()).copy();
        }
        return ItemStack.EMPTY;
    }

    public ResourceLocation getLootFactoryName(ItemStack stack) {
        return stack.get(OCComponentsJava.LOOT_DISK.get());
    }

    private List<ItemStack> collectStacks(CraftingInput crafting) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < crafting.size(); i++) {
            ItemStack stack = crafting.getItem(i);
            if (stack != null && !stack.isEmpty()) stacks.add(stack);
        }
        return stacks;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        Buffer<ItemStack> disks = Loot.disksForCycling();
        return disks.isEmpty() ? ItemStack.EMPTY : disks.head();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput crafting) {
        NonNullList<ItemStack> result = NonNullList.withSize(crafting.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < crafting.size(); slot++) {
            ItemStack stack = crafting.getItem(slot);
            if (Wrench.isWrench(stack)) {
                result.set(slot, stack.copy());
                stack.setCount(0);
            }
        }
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        Buffer<ItemStack> disks = Loot.disksForCycling();
        ItemStack[] diskStacks = new ItemStack[disks.length()];
        for (int i = 0; i < diskStacks.length; i++) diskStacks[i] = disks.apply(i);
        ingredients.add(Ingredient.of(diskStacks));
        ingredients.add(Ingredient.of(Items.get(Constants.ItemName$.MODULE$.Wrench()).createItemStack(1)));
        return ingredients;
    }
}
