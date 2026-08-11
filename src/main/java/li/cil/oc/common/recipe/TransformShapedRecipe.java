package li.cil.oc.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc.common.recipe.function.RecipeFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.List;

import static li.cil.oc.common.recipe.TransformShapelessRecipe.configureResult;

/**
 * A {@link ShapedRecipe} that applies a list of {@linkplain RecipeFunction recipe functions}.
 */
public final class TransformShapedRecipe extends ShapedRecipe {
    public static final MapCodec<TransformShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        RecipeSerializer.SHAPED_RECIPE.codec().forGetter(TransformShapedRecipe::getOriginal),
        RecipeFunction.LIST_CODEC.fieldOf("functions").forGetter(x -> x.functions)
    ).apply(instance, (r, funcs) ->
        new TransformShapedRecipe(r.getGroup(), r.category(), r.pattern, r.getResultItem(RegistryAccess.EMPTY), funcs)
    ));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformShapedRecipe> STREAM_CODEC = StreamCodec.composite(
        RecipeSerializer.SHAPED_RECIPE.streamCodec(), TransformShapedRecipe::getOriginal,
        ItemStack.STREAM_CODEC, x -> x.transformedResult,
        RecipeFunction.LIST_STREAM_CODEC, x -> x.functions,
        (r, transRes, funcs) ->
            new TransformShapedRecipe(r.getGroup(), r.category(), r.pattern, r.getResultItem(RegistryAccess.EMPTY), transRes, funcs)
    );

    public static final RecipeSerializer<TransformShapedRecipe> SERIALIZER = new BasicRecipeSerializer<>(CODEC, STREAM_CODEC);

    private final List<RecipeFunction> functions;
    private final ItemStack originalResult;
    private final ItemStack transformedResult;

    public TransformShapedRecipe(
        String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, List<RecipeFunction> functions
    ) {
        this(group, category, pattern, result, configureResult(result, functions), functions);
    }

    private TransformShapedRecipe(
        String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack originalResult, ItemStack transformedResult, List<RecipeFunction> functions
    ) {
        super(group, category, pattern, transformedResult);
        this.functions = functions;
        this.originalResult = originalResult;
        this.transformedResult = transformedResult;
    }

    private ShapedRecipe getOriginal() {
        return new ShapedRecipe(getGroup(), category(), pattern, originalResult);
    }

    @Override
    public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registryAccess) {
        var result = super.assemble(inventory, registryAccess);
        for (var function : functions) result = function.apply(inventory, result);
        return result;
    }

    @Override
    public RecipeSerializer<TransformShapedRecipe> getSerializer() {
        return SERIALIZER;
    }
}

