package li.cil.oc.common.recipe.function;

import com.mojang.serialization.MapCodec;
import li.cil.oc.server.machine.luac.LuaStateFactory;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Set the default on the resulting item.
 */
public class SetDefaultArchitecture implements RecipeFunction {
    public static final SetDefaultArchitecture INSTANCE = new SetDefaultArchitecture();
    public static final Type<SetDefaultArchitecture> TYPE = new Type<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

    private SetDefaultArchitecture() {
    }

    @Override
    public Type<?> getType() {
        return TYPE;
    }

    @Override
    public void configureResult(ItemStack result) {
        LuaStateFactory.setDefaultArch(result);
    }
}
