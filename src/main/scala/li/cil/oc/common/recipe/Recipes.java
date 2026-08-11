package li.cil.oc.common.recipe;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.recipe.function.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.function.Function;

public final class Recipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, OpenComputers.ID());

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LootDiskCyclingRecipe>> LOOTDISK_CYCLING = SERIALIZERS.register(
        "crafting_lootdisk_cycling",
        () -> new SimpleCraftingRecipeSerializer<>(LootDiskCyclingRecipe::new)
    );

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColorizeRecipe>> COLORIZE = SERIALIZERS.register("crafting_colorize", () -> itemSpecialSerializer(ColorizeRecipe::new, ColorizeRecipe::targetItem));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DecolorizeRecipe>> DECOLORIZE = SERIALIZERS.register("crafting_decolorize", () -> itemSpecialSerializer(DecolorizeRecipe::new, DecolorizeRecipe::targetItem));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TransformShapedRecipe>> TRANSFORM_SHAPED = SERIALIZERS.register("transform_shaped", () -> TransformShapedRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TransformShapelessRecipe>> TRANSFORM_SHAPELESS = SERIALIZERS.register("transform_shapeless", () -> TransformShapelessRecipe.SERIALIZER);

    private static final DeferredRegister<RecipeFunction.Type<?>> FUNCTIONS = DeferredRegister.create(RecipeFunction.REGISTRY, OpenComputers.ID());

    public static final DeferredHolder<RecipeFunction.Type<?>, RecipeFunction.Type<AddPrintLight>> ADD_PRINT_LIGHT = FUNCTIONS.register("add_print_light", () -> AddPrintLight.TYPE);
    public static final DeferredHolder<RecipeFunction.Type<?>, RecipeFunction.Type<CopyComponents>> COPY_COMPONENTS = FUNCTIONS.register("copy_components", () -> CopyComponents.TYPE);
    public static final DeferredHolder<RecipeFunction.Type<?>, RecipeFunction.Type<ReplaceEEPROM>> REPLACE_EEPROM = FUNCTIONS.register("replace_eeprom", () -> ReplaceEEPROM.TYPE);
    public static final DeferredHolder<RecipeFunction.Type<?>, RecipeFunction.Type<SetDefaultArchitecture>> SET_DEFAULT_ARCHITECTURE = FUNCTIONS.register("set_default_architecture", () -> SetDefaultArchitecture.TYPE);
    public static final DeferredHolder<RecipeFunction.Type<?>, RecipeFunction.Type<SetEEPROMData>> SET_EEPROM_DATA = FUNCTIONS.register("set_eeprom_data", () -> SetEEPROMData.TYPE);
    public static final DeferredHolder<RecipeFunction.Type<?>, RecipeFunction.Type<SetSourceMap>> SET_SOURCE_MAP = FUNCTIONS.register("set_source_map", () -> SetSourceMap.TYPE);
    public static final DeferredHolder<RecipeFunction.Type<?>, RecipeFunction.Type<SetUniqueTunnel>> SET_UNIQUE_TUNNEL = FUNCTIONS.register("set_unique_tunnel", () -> SetUniqueTunnel.TYPE);

    public static void init(IEventBus eventBus) {
        eventBus.addListener((NewRegistryEvent event) -> event.create(new RegistryBuilder<>(RecipeFunction.REGISTRY).sync(true)));
        SERIALIZERS.register(eventBus);
        FUNCTIONS.register(eventBus);
    }

    private static <T extends Recipe<?>> RecipeSerializer<T> itemSpecialSerializer(Function<ItemLike, T> ctor, Function<T, Item> getter) {
        return new BasicRecipeSerializer<>(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").xmap(ctor, getter),
            ByteBufCodecs.registry(Registries.ITEM).map(ctor, getter)
        );
    }
}
