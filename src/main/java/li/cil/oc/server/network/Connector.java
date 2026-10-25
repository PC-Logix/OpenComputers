package li.cil.oc.server.network;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Settings;
import li.cil.oc.api.network.Node;
import li.cil.oc.util.ExtendedDataComponentHolder;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;

/** Mutable energy node assembled by the server network builders. */
public interface Connector extends li.cil.oc.api.network.Connector, li.cil.oc.server.network.Node {
    double localBufferSize();
    void localBufferSize_$eq(double value);
    double localBuffer();
    void localBuffer_$eq(double value);
    Option<Distributor> distributor();
    void distributor_$eq(Option<Distributor> value);

    @Override
    default double globalBuffer() {
        Option<Distributor> distributor = distributor();
        return distributor.isDefined() ? distributor.get().globalBuffer() : localBuffer();
    }

    @Override
    default double globalBufferSize() {
        Option<Distributor> distributor = distributor();
        return distributor.isDefined() ? distributor.get().globalBufferSize() : localBufferSize();
    }

    @Override
    default double changeBuffer(double delta) {
        if (delta == 0) return 0;
        return ConnectorEnergy.changeBuffer(this, delta, Settings.get().ignorePower());
    }

    @Override
    default boolean tryChangeBuffer(double delta) {
        if (delta == 0) return true;
        return ConnectorEnergy.tryChangeBuffer(this, delta, Settings.get().ignorePower());
    }

    @Override
    default void setLocalBufferSize(double size) {
        double clampedSize = Math.max(size, 0);
        synchronized (this) {
            Option<Distributor> distributor = distributor();
            if (distributor.isDefined()) {
                Distributor connected = distributor.get();
                synchronized (connected) {
                    double oldSize = localBufferSize();
                    // Install the new size before registering a previously empty connector.
                    localBufferSize_$eq(clampedSize);
                    if (network() != null) {
                        if (oldSize <= 0 && clampedSize > 0) connected.addConnector(this);
                        else if (oldSize > 0 && clampedSize == 0) connected.removeConnector(this);
                        else connected.globalBufferSize_$eq(Math.max(connected.globalBufferSize() - oldSize + clampedSize, 0));
                    }
                    double surplus = Math.max(localBuffer() - clampedSize, 0);
                    changeBuffer(-surplus);
                    connected.changeBuffer(surplus);
                }
            } else {
                localBufferSize_$eq(clampedSize);
                localBuffer_$eq(Math.min(localBuffer(), localBufferSize()));
            }
        }
    }

    @Override
    default void onDisconnect(Node node) {
        li.cil.oc.server.network.Node.super.onDisconnect(node);
        if (node == this) {
            synchronized (this) { distributor_$eq(Option.empty()); }
        }
    }

    @Override
    default void loadData(DataComponentHolder holder) {
        li.cil.oc.server.network.Node.super.loadData(holder);
        java.util.Optional<?> charge = new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.CHARGE);
        charge.ifPresent(value -> localBuffer_$eq(((Number) value).doubleValue()));
    }

    @Override
    default void saveData(MutableDataComponentHolder holder) {
        li.cil.oc.server.network.Node.super.saveData(holder);
        holder.set(OCComponentsJava.CHARGE, localBuffer());
    }
}
