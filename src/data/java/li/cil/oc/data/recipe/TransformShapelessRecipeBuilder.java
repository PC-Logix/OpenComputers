package li.cil.oc.data.recipe;

import li.cil.oc.common.recipe.TransformShapelessRecipe;
import li.cil.oc.common.recipe.function.RecipeFunction;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/**
 * A builder for {@link TransformShapelessRecipe}s, much like {@link ShapelessRecipeBuilder}.
 */
public final class TransformShapelessRecipeBuilder extends AbstractRecipeBuilder<TransformShapelessRecipeBuilder, TransformShapelessRecipe> {
    private final NonNullList<Ingredient> ingredients = NonNullList.create();
    private final List<RecipeFunction> functions = new ArrayList<>();

    private TransformShapelessRecipeBuilder(RecipeCategory category, ItemStack result) {
        super(category, result);
    }

    public static TransformShapelessRecipeBuilder of(RecipeCategory category, ItemStack result) {
        return new TransformShapelessRecipeBuilder(category, result);
    }

    public static TransformShapelessRecipeBuilder of(RecipeCategory category, ItemLike result) {
        return new TransformShapelessRecipeBuilder(category, new ItemStack(result));
    }

    public TransformShapelessRecipeBuilder requires(Ingredient ingredient, int count) {
        for (int i = 0; i < count; i++) ingredients.add(ingredient);
        return this;
    }

    public TransformShapelessRecipeBuilder requires(Ingredient ingredient) {
        return requires(ingredient, 1);
    }

    public TransformShapelessRecipeBuilder requires(ItemLike item) {
        return requires(Ingredient.of(new ItemStack(item)));
    }

    public TransformShapelessRecipeBuilder requires(ItemLike item, int count) {
        return requires(Ingredient.of(new ItemStack(item)), count);
    }

    public TransformShapelessRecipeBuilder requires(TagKey<Item> item) {
        return requires(Ingredient.of(item));
    }

    public TransformShapelessRecipeBuilder function(RecipeFunction function) {
        this.functions.add(function);
        return this;
    }

    @Override
    protected TransformShapelessRecipe build() {
        if (functions.isEmpty()) throw new IllegalStateException("Must have at least one recipe function");
        return new TransformShapelessRecipe(group, RecipeBuilder.determineBookCategory(category), result, ingredients, functions);
    }
}
