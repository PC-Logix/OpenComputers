package li.cil.oc.util;

import li.cil.oc.common.datacomponents.Migrator;
import li.cil.oc.common.item.data.ItemData;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Option$;

import java.util.concurrent.atomic.AtomicReference;
import java.util.Optional;

/** Java implementation of the legacy custom-data component migration fallback. */
public final class ExtendedDataComponentHolderSupportJava {
    public static ExtendedDataComponentHolder convert(DataComponentHolder holder) {
        return new ExtendedDataComponentHolder(holder);
    }

    public static ExtendedMutableDataComponentHolderJava convert(MutableDataComponentHolder holder) {
        return new ExtendedMutableDataComponentHolderJava(holder);
    }
    public static <T> Optional<T> tryFallback(MutableDataComponentHolder holder, DataComponentType<T> dataComponent) {
        if (!holder.has(DataComponents.CUSTOM_DATA)) return Optional.empty();

        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());
        holder.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            data.update(tag -> {
                Option<T> migrated = Migrator.perform(dataComponent, tag, ItemData.defaultProvider());
                if (migrated.isDefined()) {
                    T value = migrated.get();
                    holder.set(dataComponent, value);
                    result.set(Optional.of(value));
                }
            });
            return data;
        });
        return result.get();
    }

    private ExtendedDataComponentHolderSupportJava() {}
}
