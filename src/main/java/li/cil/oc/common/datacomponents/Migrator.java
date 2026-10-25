package li.cil.oc.common.datacomponents;

import li.cil.oc.Settings;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import scala.Option;
import scala.Option$;

public interface Migrator<T> {
    Option<T> fromNBT(CompoundTag tag, HolderLookup.Provider provider);

    interface DataNamespace<T> extends Migrator<T> {
        Option<T> fromData(CompoundTag tag, HolderLookup.Provider provider);

        @Override
        default Option<T> fromNBT(CompoundTag tag, HolderLookup.Provider provider) {
            String key = Settings.namespace() + (Settings.namespace().endsWith(":") ? "" : ":") + "data";
            return tag.contains(key, Tag.TAG_COMPOUND)
                ? fromData(tag.getCompound(key), provider)
                : Option$.MODULE$.empty();
        }
    }

    @SuppressWarnings("unchecked")
    static <T> Option<T> perform(DataComponentType<T> type, CompoundTag nbt, HolderLookup.Provider provider) {
        Option<Migrator<?>> migrator = Migrators.map.get(type);
        if (migrator.isEmpty()) return Option$.MODULE$.empty();
        return ((Migrator<T>) migrator.get()).fromNBT(nbt, provider);
    }
}
