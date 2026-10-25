package li.cil.oc.common.recipe;

import li.cil.oc.OpenComputers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public final class Recipes {
    public static final class RecipeRegistration<R extends Recipe<?>> {
        private final Supplier<RecipeType<R>> recipeType;
        private final Supplier<RecipeSerializer<R>> serializer;

        public RecipeRegistration(Supplier<RecipeType<R>> recipeType, Supplier<RecipeSerializer<R>> serializer) {
            this.recipeType = recipeType;
            this.serializer = serializer;
        }

        public RecipeType<R> getRecipeType() {
            return recipeType.get();
        }

        public RecipeSerializer<R> getSerializer() {
            return serializer.get();
        }
    }

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, OpenComputers.ID());
    public static final DeferredRegister<RecipeType<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_TYPE, OpenComputers.ID());

    public static final RecipeRegistration<LootDiskCyclingRecipe> LOOTDISK_CYCLING = register(
            "crafting_lootdisk_cycling",
            new SimpleCraftingRecipeSerializer<>(LootDiskCyclingRecipe::new)
    );
    public static final RecipeRegistration<ColorizeRecipe> COLORIZE = register("crafting_colorize", new ItemSpecialSerializer<>(ColorizeRecipe::new, ColorizeRecipe::targetItem));
    public static final RecipeRegistration<DecolorizeRecipe> DECOLORIZE = register("crafting_decolorize", new ItemSpecialSerializer<>(DecolorizeRecipe::new, DecolorizeRecipe::targetItem));
    public static final RecipeRegistration<RobotFlagRecipe> ROBOT_FLAG = register("crafting_robot_flag", new RobotFlagRecipe.Serializer());
    public static final RecipeRegistration<ExtendedShapedRecipe> SHAPED_EXTENDED = register("crafting_shaped_extended", new ExtendedShapedRecipe.Serializer());
    public static final RecipeRegistration<ExtendedShapelessRecipe> SHAPELESS_EXTENDED = register("crafting_shapeless_extended", new ExtendedShapelessRecipe.Serializer());

    private static <R extends Recipe<?>> RecipeRegistration<R> register(String id, RecipeSerializer<R> serializer) {
        DeferredHolder<RecipeType<?>, RecipeType<R>> recipeType = RECIPES.register(id, () -> new RecipeType<>() {
            @Override
            public String toString() { return id; }
        });
        DeferredHolder<RecipeSerializer<?>, RecipeSerializer<R>> recipeSerializer = SERIALIZERS.register(id, () -> serializer);
        return new RecipeRegistration<>(
                recipeType::get,
                recipeSerializer::get
        );
    }
    
    public static void init(IEventBus eventBus) {
        RECIPES.register(eventBus);
        SERIALIZERS.register(eventBus);
    }
}
