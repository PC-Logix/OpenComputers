package li.cil.oc.server.network;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedMutableDataComponentHolder;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

/** A component that also stores and distributes connector energy. */
public interface ComponentConnector extends li.cil.oc.api.network.ComponentConnector, Component, Connector {
    @Override default void loadData(DataComponentHolder holder) {
        Component.super.loadData(holder);
        ExtendedDataComponentHolder extended = holder instanceof MutableDataComponentHolder mutable
            ? new ExtendedMutableDataComponentHolder(mutable) : new ExtendedDataComponentHolder(holder);
        var charge = extended.getComponentOptional(OCComponentsJava.CHARGE);
        charge.ifPresent(value -> localBuffer_$eq(((Number) value).doubleValue()));
    }

    @Override default void saveData(MutableDataComponentHolder holder) {
        Component.super.saveData(holder);
        holder.set(OCComponentsJava.CHARGE, localBuffer());
    }
}
