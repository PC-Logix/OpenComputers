package li.cil.oc.util;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;

import java.util.Optional;
import java.util.function.Supplier;

public class ExtendedDataComponentHolder {
    protected final DataComponentHolder holder;

    public ExtendedDataComponentHolder(DataComponentHolder holder) {
        this.holder = holder;
    }

    protected <T> Optional<T> tryFallback(DataComponentType<T> dataComponent) {
        return Optional.empty();
    }

    public <T> Optional<T> getComponentOptional(DataComponentType<T> dataComponent) {
        T value = holder.get(dataComponent);
        return value == null ? tryFallback(dataComponent) : Optional.of(value);
    }

    public <T> Optional<T> getComponentOptional(Supplier<DataComponentType<T>> supplier) {
        return getComponentOptional(supplier.get());
    }
}
