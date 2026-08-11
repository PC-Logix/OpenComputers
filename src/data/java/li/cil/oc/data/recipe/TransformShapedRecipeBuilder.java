package li.cil.oc.data.recipe;

import li.cil.oc.common.recipe.TransformShapedRecipe;
import li.cil.oc.common.recipe.function.RecipeFunction;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A builder for {@link TransformShapedRecipe}s, much like {@link ShapedRecipeBuilder}.
 */
public final class TransformShapedRecipeBuilder extends AbstractRecipeBuilder<TransformShapedRecipeBuilder, TransformShapedRecipe> {
    private final List<String> rows = new ArrayList<>();
    private final Map<Character, Ingredient> key = new LinkedHashMap<>();
    private final List<RecipeFunction> functions = new ArrayList<>();

    private TransformShapedRecipeBuilder(RecipeCategory category, ItemStack result) {
        super(category, result);
    }

    public static TransformShapedRecipeBuilder of(RecipeCategory category, ItemStack result) {
        return new TransformShapedRecipeBuilder(category, result);
    }

    public static TransformShapedRecipeBuilder of(RecipeCategory category, ItemLike result) {
        return new TransformShapedRecipeBuilder(category, new ItemStack(result));
    }

    public TransformShapedRecipeBuilder define(char key, Ingredient ingredient) {
        if (this.key.containsKey(key)) throw new IllegalArgumentException("Symbol '" + key + "' is already defined!");
        if (key == ' ') throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");

        this.key.put(key, ingredient);
        return this;
    }

    public TransformShapedRecipeBuilder define(char key, TagKey<Item> tag) {
        return this.define(key, Ingredient.of(tag));
    }

    public TransformShapedRecipeBuilder define(char key, ItemLike item) {
        return define(key, Ingredient.of(item));
    }

    public TransformShapedRecipeBuilder pattern(String pattern) {
        if (!rows.isEmpty() && pattern.length() != rows.getFirst().length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        } else {
            rows.add(pattern);
            return this;
        }
    }

    public TransformShapedRecipeBuilder function(RecipeFunction function) {
        functions.add(function);
        return this;
    }

    @Override
    protected TransformShapedRecipe build() {
        if (functions.isEmpty()) throw new IllegalStateException("Must have at least one recipe function");
        return new TransformShapedRecipe(group, RecipeBuilder.determineBookCategory(category), ShapedRecipePattern.of(key, rows), result, functions);
    }
}
