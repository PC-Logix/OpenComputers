package li.cil.oc.common.recipe.function;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.datacomponents.OCComponents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

import java.util.UUID;

/**
 * Set the {@link OCComponents#TUNNEL()} to a random, unique ID.
 */
public class SetUniqueTunnel implements RecipeFunction {
    public static final SetUniqueTunnel INSTANCE = new SetUniqueTunnel();
    public static final Type<SetUniqueTunnel> TYPE = new Type<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

    private SetUniqueTunnel() {
    }

    @Override
    public Type<?> getType() {
        return TYPE;
    }

    @Override
    public ItemStack apply(CraftingInput container, ItemStack result) {
        result.set(OCComponents.TUNNEL(), UUID.randomUUID().toString());
        return result;
    }
}
