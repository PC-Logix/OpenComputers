package li.cil.oc.common.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

/**
 * Applies a flag to a robot while preserving the robot ItemStack verbatim.
 *
 * <p>The general extended recipe wrapper treats any device-to-device recipe as
 * an EEPROM swap. A cosmetic recipe must not enter that path: it only applies
 * the result's component patch to the input robot.</p>
 */
public final class RobotFlagRecipe implements CraftingRecipe {
    private final ShapelessRecipe wrapped;

    public RobotFlagRecipe(ShapelessRecipe wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean matches(@NonNull CraftingInput inv, @NonNull Level level) {
        return wrapped.matches(inv, level);
    }

    @Override
    public ItemStack assemble(@NonNull CraftingInput inv, @NotNull HolderLookup.Provider provider) {
        ItemStack recipeResult = wrapped.assemble(inv, provider);
        if (recipeResult.isEmpty()) return ItemStack.EMPTY;

        for (int slot = 0; slot < inv.size(); slot++) {
            ItemStack input = inv.getItem(slot);
            if (!input.isEmpty() && input.is(recipeResult.getItem())) {
                ItemStack output = input.copy();
                output.setCount(recipeResult.getCount());
                output.applyComponents(recipeResult.getComponentsPatch());
                return output;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return wrapped.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem(@NotNull HolderLookup.Provider provider) {
        return wrapped.getResultItem(provider);
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
    public RecipeSerializer<?> getSerializer() {
        return Recipes.ROBOT_FLAG.getSerializer();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public CraftingBookCategory category() {
        return wrapped.category();
    }

    @Override
    public String getGroup() {
        return wrapped.getGroup();
    }

    @Override
    public boolean showNotification() {
        return wrapped.showNotification();
    }

    public static final class Serializer implements RecipeSerializer<RobotFlagRecipe> {
        private static final MapCodec<RobotFlagRecipe> CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(RobotFlagRecipe::new, recipe -> recipe.wrapped);
        private static final StreamCodec<RegistryFriendlyByteBuf, RobotFlagRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(RobotFlagRecipe::new, recipe -> recipe.wrapped);

        @Override
        public @NotNull MapCodec<RobotFlagRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, RobotFlagRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
