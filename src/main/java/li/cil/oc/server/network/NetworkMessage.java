package li.cil.oc.server.network;

import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;

/** Message envelope used while dispatching a network event. */
final class NetworkMessage implements Message {
    private final Node source;
    private final String name;
    private final Object[] data;
    private boolean canceled;

    NetworkMessage(Node source, String name, Object[] data) {
        this.source = source;
        this.name = name;
        this.data = data;
    }

    @Override public Node source() { return source; }
    @Override public String name() { return name; }
    @Override public Object[] data() { return data; }
    public boolean isCanceled() { return canceled; }
    public void isCanceled_$eq(boolean canceled) { this.canceled = canceled; }
    @Override public void cancel() { canceled = true; }
}
