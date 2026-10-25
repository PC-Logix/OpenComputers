package li.cil.oc.util;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.common.Tier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;
import scala.Option;
import scala.Option$;
import scala.collection.immutable.ArraySeq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ItemUtils {
    private static final Random rng = new Random();

    private ItemUtils() {}

    @Nullable
    public static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static Option<String> getDisplayName(CompoundTag nbt) {
        if (nbt.contains("display")) {
            CompoundTag display = nbt.getCompound("display");
            if (display.contains("Name")) return Option$.MODULE$.apply(display.getString("Name"));
        }
        return Option$.MODULE$.empty();
    }

    public static void setDisplayName(CompoundTag nbt, String name) {
        if (!nbt.contains("display")) nbt.put("display", new CompoundTag());
        nbt.getCompound("display").putString("Name", name);
    }

    public static int getHarvestLevel(BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 1;
        return 0;
    }

    @Nullable
    public static String getHarvestTool(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return "pickaxe";
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) return "axe";
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return "shovel";
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) return "hoe";
        return null;
    }

    public static int caseTier(ItemStack stack) {
        Object descriptor = Items.get(stack);
        if (descriptor == Items.get(Constants.BlockName$.MODULE$.CaseTier1())) return Tier.One;
        if (descriptor == Items.get(Constants.BlockName$.MODULE$.CaseTier2())) return Tier.Two;
        if (descriptor == Items.get(Constants.BlockName$.MODULE$.CaseTier3())) return Tier.Three;
        if (descriptor == Items.get(Constants.BlockName$.MODULE$.CaseTier4())) return Tier.Four;
        if (descriptor == Items.get(Constants.BlockName$.MODULE$.CaseCreative())) return Tier.Five;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.MicrocontrollerCaseTier1())) return Tier.One;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.MicrocontrollerCaseTier2())) return Tier.Two;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.MicrocontrollerCaseTier3())) return Tier.Three;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.MicrocontrollerCaseCreative())) return Tier.Five;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.DroneCaseTier1())) return Tier.One;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.DroneCaseTier2())) return Tier.Two;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.DroneCaseTier3())) return Tier.Three;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.DroneCaseCreative())) return Tier.Five;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.ServerTier1())) return Tier.One;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.ServerTier2())) return Tier.Two;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.ServerTier3())) return Tier.Three;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.ServerTier4())) return Tier.Four;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.ServerCreative())) return Tier.Five;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.TabletCaseTier1())) return Tier.One;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.TabletCaseTier2())) return Tier.Two;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.TabletCaseTier3())) return Tier.Three;
        if (descriptor == Items.get(Constants.ItemName$.MODULE$.TabletCaseCreative())) return Tier.Five;
        return Tier.None;
    }

    public static String caseNameWithTierSuffix(String name, int tier) {
        return name + (tier == Tier.Five ? "creative" : Integer.toString(tier + 1));
    }

    public static CompoundTag loadTag(byte[] data) {
        try {
            return NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] saveStack(ItemStack stack, HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider current = ServerLifecycleHooks.getCurrentServer().registryAccess();
        stack.save(current, tag);
        return saveTag(tag);
    }

    public static byte[] saveTag(CompoundTag tag) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(tag, output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return output.toByteArray();
    }

    public static ItemStack[] getIngredients(RecipeManager manager, ItemStack stack) {
        try {
            List<ItemStack> ingredients = null;
            int outputSize = 1;
            for (var holder : manager.getAllRecipesFor(RecipeType.CRAFTING)) {
                Recipe<?> recipe = holder.value();
                ItemStack result = recipe.getResultItem(null);
                if (result.isEmpty() || !ItemStack.isSameItem(result, stack)) continue;
                if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) continue;

                ArrayList<ItemStack> inputs = new ArrayList<>();
                for (Ingredient ingredient : recipe.getIngredients()) {
                    ItemStack[] choices = ingredient.getItems();
                    if (choices.length > 0) inputs.add(choices[rng.nextInt(choices.length)]);
                }
                int count = recipe.getResultItem(ServerLifecycleHooks.getCurrentServer().registryAccess()).getCount();
                inputs.removeIf(input -> input.isEmpty() || input.getCount() / count <= 0 || input.getItem() instanceof BucketItem);
                boolean blacklisted = inputs.stream().anyMatch(input -> {
                    Item item = input.getItem();
                    var key = item instanceof BlockItem blockItem
                            ? BuiltInRegistries.BLOCK.getKey(blockItem.getBlock())
                            : BuiltInRegistries.ITEM.getKey(item);
                    return Settings.get().disassemblerInputBlacklist().contains(key);
                });
                if (!blacklisted) {
                    ingredients = inputs;
                    outputSize = count;
                    break;
                }
            }
            if (ingredients == null) return new ItemStack[0];
            if (ingredients.stream().anyMatch(ingredient -> ItemStack.isSameItem(ingredient, stack))) return new ItemStack[0];

            ArrayList<ItemStack> merged = new ArrayList<>();
            for (ItemStack ingredient : ingredients) {
                ItemStack existing = merged.stream().filter(item -> ItemStack.isSameItem(ingredient, item)).findFirst().orElse(null);
                if (existing == null) merged.add(ingredient.copy());
                else existing.grow(ingredient.getCount());
            }
            for (ItemStack item : merged) item.setCount(item.getCount() / outputSize);
            ArrayList<ItemStack> distinct = new ArrayList<>();
            for (ItemStack ingredient : merged) {
                int size = Math.max(1, ingredient.getCount());
                ingredient.setCount(1);
                for (int i = 0; i < size; i++) distinct.add(ingredient.copy());
            }
            return distinct.toArray(ItemStack[]::new);
        } catch (Throwable t) {
            OpenComputers.log().warn("Whoops, something went wrong when trying to figure out an item's parts.", t);
            return new ItemStack[0];
        }
    }
}
