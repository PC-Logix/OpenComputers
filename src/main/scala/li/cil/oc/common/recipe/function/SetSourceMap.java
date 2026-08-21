package li.cil.oc.common.recipe.function;

import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.common.datacomponents.OCComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Store an item in the recipe input in the {@link OCComponents#SOURCE_MAP_ITEM()} component.
 *
 * @param map The map item to store.
 */
public record SetSourceMap(Ingredient map) implements RecipeFunction {
    public static final RecipeFunction.Type<SetSourceMap> TYPE = new RecipeFunction.Type<>(
        Ingredient.CODEC_NONEMPTY.fieldOf("from").xmap(SetSourceMap::new, SetSourceMap::map),
        Ingredient.CONTENTS_STREAM_CODEC.map(SetSourceMap::new, SetSourceMap::map)
    );

    @Override
    public Type<?> getType() {
        return TYPE;
    }

    @Override
    public ItemStack apply(CraftingInput container, ItemStack result) {
        for (var item : container.items()) {
            if (map.test(item)) {
                result.set(OCComponents.SOURCE_MAP_ITEM().get(), ImmutableItemStack.copyOf(item));
                break;
            }
        }

        return result;
    }
}
