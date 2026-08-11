package li.cil.oc.common.recipe.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import li.cil.oc.common.recipe.TransformShapedRecipe;
import li.cil.oc.common.recipe.TransformShapelessRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

import java.util.List;

/**
 * A function that is applied to the result of a recipe, mutating it in some way. These can be used from within a recipe
 * JSON file to define basic dynamic recipes, rather than having to fall back to Java.
 * <p>
 * For instance, the recipe to dye a floppy, is defined as a basic shaped recipes plus an additional
 * {@link CopyComponents} function, that copies all components but the color from the floppy.
 * <p>
 * The design and implementation of these are very similar to Minecraft's existing {@linkplain LootItemFunction loot
 * functions}.
 *
 * @see TransformShapedRecipe
 * @see TransformShapelessRecipe
 */
public interface RecipeFunction {
    /**
     * The registry where {@link RecipeFunction}s are registered.
     */
    ResourceKey<Registry<Type<?>>> REGISTRY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("opencomputers", "recipe_function"));

    /**
     * The codec to read and write {@link RecipeFunction}s with.
     */
    Codec<RecipeFunction> CODEC = Codec
        .lazyInitialized(() -> RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registryOrThrow(REGISTRY).byNameCodec())
        .dispatch(RecipeFunction::getType, Type::codec);

    /**
     * A codec for a list of functions.
     */
    Codec<List<RecipeFunction>> LIST_CODEC = CODEC.listOf(1, Integer.MAX_VALUE);

    /**
     * The {@link StreamCodec} equivalent of {@link #CODEC}.
     */
    StreamCodec<RegistryFriendlyByteBuf, RecipeFunction> STREAM_CODEC = ByteBufCodecs.registry(REGISTRY).dispatch(RecipeFunction::getType, Type::streamCodec);

    /**
     * The {@link StreamCodec} equivalent of {@link #LIST_CODEC}.
     */
    StreamCodec<RegistryFriendlyByteBuf, List<RecipeFunction>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());

    /**
     * Get the type of this recipe function.
     *
     * @return The type of this recipe function.
     */
    Type<?> getType();

    /**
     * Modify the static result of this recipe.
     *
     * @param result The stack to modify.
     */
    default void configureResult(ItemStack result) {
    }

    /**
     * Apply this recipe function, modifying the result item.
     *
     * @param container The current crafting container.
     * @param result    The result item to modify. This may be mutated in place.
     * @return The new result item. This may be {@code result}.
     */
    default ItemStack apply(CraftingInput container, ItemStack result) {
        return result;
    }

    /**
     * Properties about a type of {@link RecipeFunction}. These are stored in {@linkplain #REGISTRY a Minecraft
     * registry}, and returned by {@link #getType()}.
     *
     * @param codec       The codec to read and write this class of recipe functions with.
     * @param streamCodec The network codec to read and write this class of recipe functions with.
     * @param <T>         The type of recipe function.
     */
    record Type<T extends RecipeFunction>(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
    }
}

