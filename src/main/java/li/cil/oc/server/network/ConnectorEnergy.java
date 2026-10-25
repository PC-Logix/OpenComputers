package li.cil.oc.server.network;

import scala.Option;

/** Energy accounting shared by the connector API and focused tests. */
final class ConnectorEnergy {
    private ConnectorEnergy() { }

    static double changeBuffer(Connector connector, double delta, boolean ignorePower) {
        if (delta == 0) return 0;
        if (ignorePower) return delta < 0 ? 0 : delta;

        synchronized (connector) {
            Option<Distributor> distributor = connector.distributor();
            if (distributor.isDefined()) {
                Distributor connected = distributor.get();
                synchronized (connected) {
                    return connected.changeBuffer(change(connector, delta));
                }
            }
            return change(connector, delta);
        }
    }

    private static double change(Connector connector, double delta) {
        if (connector.localBufferSize() <= 0) return delta;
        double oldBuffer = connector.localBuffer();
        double updated = oldBuffer + delta;
        double remaining = 0;
        if (updated < 0) {
            remaining = updated;
            updated = 0;
        } else if (updated > connector.localBufferSize()) {
            remaining = updated - connector.localBufferSize();
            updated = connector.localBufferSize();
        }
        connector.localBuffer_$eq(updated);
        if (updated != oldBuffer) {
            Option<Distributor> distributor = connector.distributor();
            if (distributor.isDefined()) {
                Distributor connected = distributor.get();
                connected.globalBuffer_$eq(Math.max(0, Math.min(connected.globalBufferSize(),
                    connected.globalBuffer() - oldBuffer + updated)));
            }
        }
        return remaining;
    }

    static boolean tryChangeBuffer(Connector connector, double delta, boolean ignorePower) {
        if (delta == 0) return true;
        if (ignorePower) return delta < 0;

        synchronized (connector) {
            Option<Distributor> distributor = connector.distributor();
            if (distributor.isDefined()) {
                Distributor connected = distributor.get();
                synchronized (connected) {
                    if (connector.localBuffer() > connector.localBufferSize()) {
                        connected.changeBuffer(connector.localBuffer() - connector.localBufferSize());
                        connector.localBuffer_$eq(connector.localBufferSize());
                    }
                    double newGlobalBuffer = connector.globalBuffer() + delta;
                    return (delta > 0 || newGlobalBuffer >= 0)
                        && (delta < 0 || newGlobalBuffer <= connector.globalBufferSize())
                        && connected.changeBuffer(delta) == 0;
                }
            }
            double newLocalBuffer = connector.localBuffer() + delta;
            if ((delta < 0 && newLocalBuffer < 0) || (delta > 0 && newLocalBuffer > connector.localBufferSize())) return false;
            connector.localBuffer_$eq(newLocalBuffer);
            return true;
        }
    }
}
