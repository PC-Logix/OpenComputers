package li.cil.oc.common.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

record BasicRecipeSerializer<T extends Recipe<?>>(
    MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec
) implements RecipeSerializer<T> {
}
