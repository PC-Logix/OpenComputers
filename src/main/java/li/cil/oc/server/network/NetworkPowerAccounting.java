package li.cil.oc.server.network;

import li.cil.oc.Settings;
import scala.Option$;
import scala.collection.Iterator;
import scala.collection.mutable.ArrayBuffer;

/** Shared connector registration and network buffer accounting. */
public final class NetworkPowerAccounting {
    private NetworkPowerAccounting() {}

    public static void addConnector(NetworkCore network, ArrayBuffer<Connector> connectors,
                                    Distributor distributor, Connector connector) {
        if (connector.localBufferSize() > 0) {
            if (connectors.contains(connector)) throw new AssertionError("assertion failed");
            connectors.$plus$eq(connector);
            network.globalBuffer_$eq(network.globalBuffer() + connector.localBuffer());
            network.globalBufferSize_$eq(network.globalBufferSize() + connector.localBufferSize());
        }
        connector.distributor_$eq(Option$.MODULE$.apply(distributor));
    }

    public static void removeConnector(NetworkCore network, ArrayBuffer<Connector> connectors, Connector connector) {
        if (connector.localBufferSize() > 0) {
            if (!connectors.contains(connector)) throw new AssertionError("assertion failed");
            connectors.$minus$eq(connector);
            network.globalBuffer_$eq(network.globalBuffer() - connector.localBuffer());
            network.globalBufferSize_$eq(network.globalBufferSize() - connector.localBufferSize());
        }
    }

    public static double changeBuffer(NetworkCore network, ArrayBuffer<Connector> connectors, double delta) {
        if (delta == 0) return 0;
        if (Settings.get().ignorePower()) return delta < 0 ? 0 : delta;

        synchronized (network) {
            double oldBuffer = network.globalBuffer();
            network.globalBuffer_$eq(Math.min(Math.max(oldBuffer + delta, 0), network.globalBufferSize()));
            if (network.globalBuffer() == oldBuffer) return delta;

            double remaining = delta < 0 ? -delta : delta;
            Iterator<Connector> iterator = connectors.iterator();
            while (iterator.hasNext() && remaining > 0) {
                Connector connector = iterator.next();
                if (delta < 0) {
                    double localBuffer = connector.localBuffer();
                    if (localBuffer > 0) {
                        if (localBuffer < remaining) {
                            remaining -= localBuffer;
                            connector.localBuffer_$eq(0);
                        } else {
                            connector.localBuffer_$eq(localBuffer - remaining);
                            remaining = 0;
                        }
                    }
                } else {
                    double localBuffer = connector.localBuffer();
                    double localBufferSize = connector.localBufferSize();
                    if (localBuffer < localBufferSize) {
                        double space = localBufferSize - localBuffer;
                        if (space < remaining) {
                            remaining -= space;
                            connector.localBuffer_$eq(localBufferSize);
                        } else {
                            connector.localBuffer_$eq(localBuffer + remaining);
                            remaining = 0;
                        }
                    }
                }
            }
            return delta < 0 ? -remaining : remaining;
        }
    }
}
