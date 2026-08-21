package li.cil.oc.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc.common.recipe.function.RecipeFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.List;

/**
 * A {@link ShapelessRecipe} that applies a list of {@linkplain RecipeFunction recipe functions}.
 */
public final class TransformShapelessRecipe extends ShapelessRecipe {
    public static final MapCodec<TransformShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        RecipeSerializer.SHAPELESS_RECIPE.codec().forGetter(TransformShapelessRecipe::getOriginal),
        RecipeFunction.LIST_CODEC.fieldOf("functions").forGetter(x -> x.functions)
    ).apply(instance, (r, funcs) ->
        new TransformShapelessRecipe(r.getGroup(), r.category(), r.getResultItem(RegistryAccess.EMPTY), r.getIngredients(), funcs)
    ));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
        RecipeSerializer.SHAPELESS_RECIPE.streamCodec(), TransformShapelessRecipe::getOriginal,
        ItemStack.OPTIONAL_STREAM_CODEC, x -> x.transformedResult,
        RecipeFunction.LIST_STREAM_CODEC, x -> x.functions,
        (r, transRes, funcs) ->
            new TransformShapelessRecipe(r.getGroup(), r.category(), r.getResultItem(RegistryAccess.EMPTY), transRes, r.getIngredients(), funcs)
    );

    public static final RecipeSerializer<TransformShapelessRecipe> SERIALIZER = new BasicRecipeSerializer<>(CODEC, STREAM_CODEC);

    private final List<RecipeFunction> functions;
    private final ItemStack originalResult;
    private final ItemStack transformedResult;

    public TransformShapelessRecipe(
        String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients, List<RecipeFunction> functions
    ) {
        this(group, category, result, configureResult(result, functions), ingredients, functions);
    }

    private TransformShapelessRecipe(
        String group, CraftingBookCategory category, ItemStack originalResult, ItemStack transformedResult, NonNullList<Ingredient> ingredients,
        List<RecipeFunction> functions
    ) {
        super(group, category, transformedResult, ingredients);
        this.originalResult = originalResult;
        this.transformedResult = transformedResult;
        this.functions = functions;
    }

    private ShapelessRecipe getOriginal() {
        return new ShapelessRecipe(getGroup(), category(), originalResult, getIngredients());
    }

    static ItemStack configureResult(ItemStack stack, List<RecipeFunction> functions) {
        var result = stack.copy();
        for (var function : functions) function.configureResult(result);
        return result;
    }

    @Override
    public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registryAccess) {
        var result = super.assemble(inventory, registryAccess);
        for (var function : functions) result = function.apply(inventory, result);
        return result;
    }

    @Override
    public RecipeSerializer<TransformShapelessRecipe> getSerializer() {
        return SERIALIZER;
    }
}

