package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.blockentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.util.ExtendedDataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

/** Shared redstone wake-up settings and notification callbacks. */
public interface RedstoneSignaller extends ManagedEnvironment {
    @SuppressWarnings("unchecked")
    static DataComponentType<Integer> wakeThresholdType() {
        return (DataComponentType<Integer>) (DataComponentType<?>) OCComponentsJava.WAKE_THRESHOLD.get();
    }
    @Override Component node();
    @Override default void onConnect(Node node) { }
    @Override default void onDisconnect(Node node) { }
    @Override default void onMessage(Message message) { }
    int wakeThreshold();
    void wakeThreshold_$eq(int value);
    boolean wakeNeighborsOnly();
    void wakeNeighborsOnly_$eq(boolean value);

    @Callback(direct = true, doc = "function():number -- Get the current wake-up threshold.")
    default Object[] getWakeThreshold(Context context, Arguments args) {
        return li.cil.oc.util.ResultWrapper.result(wakeThreshold());
    }

    @Callback(doc = "function(threshold:number):number -- Set the wake-up threshold.")
    default Object[] setWakeThreshold(Context context, Arguments args) {
        int previous = wakeThreshold();
        wakeThreshold_$eq(args.checkInteger(0));
        return li.cil.oc.util.ResultWrapper.result(previous);
    }

    default void onRedstoneChanged(RedstoneChangedEventArgs args) {
        RedstoneSignallerJava.onRedstoneChanged(node(), wakeThreshold(), wakeNeighborsOnly(), args);
    }

    default void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        wakeThreshold_$eq(new ExtendedDataComponentHolder(holder).getComponentOptional(wakeThresholdType()).orElse(0));
    }

    default void saveData(MutableDataComponentHolder holder) {
        if (wakeThreshold() > 0) holder.set(wakeThresholdType(), wakeThreshold());
        else holder.remove(wakeThresholdType());
    }
}
