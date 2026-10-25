package li.cil.oc.common.recipe;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.mojang.serialization.MapCodec;
import li.cil.oc.OpenComputers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

public class ItemSpecialSerializer<T extends Recipe<?>> implements RecipeSerializer<T> {

    private static final ResourceLocation GENERATED_ID = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "special");

    private final BiFunction<ResourceLocation, ItemLike, T> ctor;
    private final Function<T, Item> getter;
    private final MapCodec<T> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

    public ItemSpecialSerializer(BiFunction<ResourceLocation, ItemLike, T> ctor, Function<T, Item> getter) {
        this.ctor = ctor;
        this.getter = getter;
        this.codec = BuiltInRegistries.ITEM.byNameCodec()
                .fieldOf("item")
                .xmap(item -> ctor.apply(GENERATED_ID, item), getter);
        this.streamCodec = ByteBufCodecs.registry(Registries.ITEM)
                .map(item -> ctor.apply(GENERATED_ID, item), getter);
    }

    @Override
    @NotNull
    public MapCodec<T> codec() {
        return codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
        return streamCodec;
    }
}
