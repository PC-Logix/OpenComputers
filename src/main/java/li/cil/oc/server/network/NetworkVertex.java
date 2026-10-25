package li.cil.oc.server.network;

import scala.collection.mutable.ArrayBuffer;

/** A node and the graph edges connected to it. */
final class NetworkVertex {
    public final Node data;
    public final ArrayBuffer<NetworkEdge> edges = new ArrayBuffer<>();

    public NetworkVertex(Node data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return data + " [" + edges.length() + "]";
    }
}
