package li.cil.oc.data.recipe;


import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An abstract base class for creating recipes, in the style of {@link RecipeBuilder}.
 *
 * @param <S> The type of this class.
 * @param <O> The output of this builder.
 *
 */
public abstract class AbstractRecipeBuilder<S extends AbstractRecipeBuilder<S, O>, O extends Recipe<?>> implements RecipeBuilder {
    protected final RecipeCategory category;
    protected final ItemStack result;
    protected String group = "";
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    protected AbstractRecipeBuilder(RecipeCategory category, ItemStack result) {
        this.category = category;
        this.result = result;
    }

    @Override
    public Item getResult() {
        return result.getItem();
    }

    /**
     * Set the group for this recipe.
     *
     * @param group The new group.
     * @return This object, for chaining.
     */
    @Override
    public final S group(String group) {
        this.group = group;
        return self();
    }

    /**
     * Add a criterion to this recipe.
     *
     * @param name      The name of the criterion.
     * @param criterion The criterion to add.
     * @return This object, for chaining.
     */
    @Override
    public final S unlockedBy(String name, Criterion<?> criterion) {
        criteria.put(name, criterion);
        return self();
    }

    /**
     * Convert this builder into the output ({@link O}) recipe.
     *
     * @return The built recipe.
     */
    protected abstract O build();

    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        if (criteria.isEmpty()) throw new IllegalStateException("No way of obtaining recipe " + id);
        var advancement = output.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(AdvancementRequirements.Strategy.OR);
        for (var entry : criteria.entrySet()) advancement.addCriterion(entry.getKey(), entry.getValue());

        output.accept(id, build(), advancement.build(id.withPrefix("recipes/" + category.getFolderName() + "/")));
    }

    @SuppressWarnings("unchecked")
    private S self() {
        return (S) this;
    }
}
