package li.cil.oc.common.recipe.function;

import com.mojang.serialization.Codec;
import li.cil.oc.common.item.data.PrintData;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/**
 * Add the supplied light level to a print.
 *
 * @param light The light level to add.
 */
public record AddPrintLight(int light) implements RecipeFunction {
    private static final int MAX_LIGHT = 15;
    public static final RecipeFunction.Type<AddPrintLight> TYPE = new RecipeFunction.Type<>(
        Codec.INT.xmap(AddPrintLight::new, AddPrintLight::light).fieldOf("light"),
        ByteBufCodecs.VAR_INT.map(AddPrintLight::new, AddPrintLight::light).cast()
    );

    @Override
    public Type<?> getType() {
        return TYPE;
    }

    @Override
    public ItemStack apply(CraftingInput container, ItemStack result) {
        var print = new PrintData(result);
        var oldLevel = print.lightLevel();

        // Crafting wouldn't change anything, prevent accidental resource loss.
        if (oldLevel == MAX_LIGHT) return ItemStack.EMPTY;

        print.lightLevel_$eq(Math.min(MAX_LIGHT, oldLevel + this.light()));
        print.saveData(result);
        return result;
    }
}
