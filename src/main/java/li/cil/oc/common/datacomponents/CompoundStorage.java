package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Option$;

public final class CompoundStorage implements MutableDataComponentHolder {
    public static final CompoundStorage EMPTY = new CompoundStorage();
    public static final Codec<CompoundStorage> CODEC = DataComponentMap.CODEC.xmap(CompoundStorage::new, CompoundStorage::getComponents);
    public static final Codec<Option<CompoundStorage>> OPTION_CODEC = CODEC.xmap(
        storage -> storage.isEmpty() ? Option$.MODULE$.empty() : Option$.MODULE$.apply(storage),
        option -> option.isDefined() ? option.get() : EMPTY);
    public static final StreamCodec<RegistryFriendlyByteBuf, CompoundStorage> STREAM_CODEC = DataComponentPatch.STREAM_CODEC
        .map(patch -> new CompoundStorage().andApply(patch), CompoundStorage::toPatch);
    public static final StreamCodec<RegistryFriendlyByteBuf, Option<CompoundStorage>> OPTION_STREAM_CODEC = STREAM_CODEC.map(
        storage -> storage.isEmpty() ? Option$.MODULE$.empty() : Option$.MODULE$.apply(storage),
        option -> option.isDefined() ? option.get() : EMPTY);

    private final PatchedDataComponentMap components;

    public CompoundStorage() {
        this(DataComponentMap.EMPTY);
    }

    public CompoundStorage(DataComponentMap original) {
        components = new PatchedDataComponentMap(original);
    }

    public CompoundStorage(CompoundTag tag) {
        this(DataComponentMap.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(tag)).build());
    }

    public boolean isEmpty() {
        return components.isEmpty();
    }

    @Override
    public <T> T set(DataComponentType<? super T> componentType, T value) {
        return components.set(componentType, value);
    }

    @Override
    public <T> T remove(DataComponentType<? extends T> componentType) {
        return components.remove(componentType);
    }

    @Override
    public void applyComponents(DataComponentPatch patch) {
        components.applyPatch(patch);
    }

    @Override
    public void applyComponents(DataComponentMap values) {
        components.setAll(values);
    }

    public CompoundStorage andApply(DataComponentPatch patch) {
        applyComponents(patch);
        return this;
    }

    public DataComponentPatch toPatch() {
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        components.forEach(builder::set);
        return builder.build();
    }

    @Override
    public DataComponentMap getComponents() {
        return components;
    }
}
