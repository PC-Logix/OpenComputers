package li.cil.oc.util;

import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Function1;
import scala.runtime.BoxedUnit;

import java.util.Optional;
import java.util.function.Supplier;

public class ExtendedMutableDataComponentHolderJava extends ExtendedDataComponentHolder {
    private final MutableDataComponentHolder mutableHolder;

    public ExtendedMutableDataComponentHolderJava(MutableDataComponentHolder holder) {
        super(holder);
        mutableHolder = holder;
    }

    @Override
    protected <T> Optional<T> tryFallback(DataComponentType<T> dataComponent) {
        return ExtendedDataComponentHolderSupportJava.tryFallback(mutableHolder, dataComponent);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setUnitComponent(Supplier<?> dataComponent, boolean value) {
        Supplier<DataComponentType<BoxedUnit>> unitComponent = (Supplier) dataComponent;
        if (value) mutableHolder.set(unitComponent, BoxedUnit.UNIT);
        else mutableHolder.remove(unitComponent);
    }

    public <T> T setComponent(DataComponentType<T> dataComponent, T value) {
        return mutableHolder.set(dataComponent, value);
    }

    public <T> T setComponent(DataComponentType<T> dataComponent, Option<T> value) {
        return value.isDefined() ? mutableHolder.set(dataComponent, value.get()) : mutableHolder.remove(dataComponent);
    }

    public <T> T setComponent(Supplier<DataComponentType<T>> dataComponent, T value) {
        return mutableHolder.set(dataComponent, value);
    }

    public <T> T setComponent(Supplier<DataComponentType<T>> dataComponent, Option<T> value) {
        return value.isDefined() ? mutableHolder.set(dataComponent, value.get()) : mutableHolder.remove(dataComponent);
    }

    public <T> T setComponent(DataComponentType<T> dataComponent, Optional<T> value) {
        return value.isPresent() ? mutableHolder.set(dataComponent, value.get()) : mutableHolder.remove(dataComponent);
    }

    public <T> T setComponent(Supplier<DataComponentType<T>> dataComponent, Optional<T> value) {
        return value.isPresent() ? mutableHolder.set(dataComponent, value.get()) : mutableHolder.remove(dataComponent);
    }

    public <T> T removeComponent(DataComponentType<T> dataComponent) {
        return mutableHolder.remove(dataComponent);
    }

    public <T, U> T updateComponent(DataComponentType<T> dataComponent, T defaultValue, Function1<T, U> update) {
        return mutableHolder.update(dataComponent, defaultValue, value -> {
            update.apply(value);
            return value;
        });
    }

    public <T, U> T updateComponent(Supplier<DataComponentType<T>> dataComponent, T defaultValue, Function1<T, U> update) {
        return mutableHolder.update(dataComponent, defaultValue, value -> {
            update.apply(value);
            return value;
        });
    }
}
