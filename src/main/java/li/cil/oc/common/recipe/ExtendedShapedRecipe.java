package li.cil.oc.common.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ExtendedShapedRecipe implements CraftingRecipe {
    private final ShapedRecipe wrapped;

    public ExtendedShapedRecipe(ShapedRecipe wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean matches(@NotNull CraftingInput inv, @NotNull Level level) {
        return wrapped.matches(inv, level);
    }

    @Override
    @NotNull
    public ItemStack assemble(@NotNull CraftingInput inv, @NotNull HolderLookup.Provider registries) {
        return ExtendedRecipe.addNBTToResult(this, wrapped.assemble(inv, registries), inv, registries);
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return wrapped.canCraftInDimensions(w, h);
    }

    @Override
    public ItemStack getResultItem(@NotNull HolderLookup.Provider registries) {
        return wrapped.getResultItem(registries);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        return wrapped.getRemainingItems(inv);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return wrapped.getIngredients();
    }

    @Override
    @NotNull
    public RecipeSerializer<?> getSerializer() {
        return Recipes.SHAPED_EXTENDED.getSerializer();
    }

    @Override
    @NotNull
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    @NotNull
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public String getGroup() {
        return wrapped.getGroup();
    }

    @Override
    public boolean showNotification() {
        return wrapped.showNotification();
    }

    public int getWidth() {
        return wrapped.getWidth();
    }

    public int getHeight() {
        return wrapped.getHeight();
    }

    public static final class Serializer implements RecipeSerializer<ExtendedShapedRecipe> {
        private static final MapCodec<ExtendedShapedRecipe> CODEC =
                ShapedRecipe.Serializer.CODEC.xmap(ExtendedShapedRecipe::new, recipe -> recipe.wrapped);
        private static final StreamCodec<RegistryFriendlyByteBuf, ExtendedShapedRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPED_RECIPE.streamCodec().map(ExtendedShapedRecipe::new, recipe -> recipe.wrapped);

        @Override
        public MapCodec<ExtendedShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ExtendedShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
