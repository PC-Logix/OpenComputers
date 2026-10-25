package li.cil.oc.server.network;

/** An undirected connection between two network graph vertices. */
final class NetworkEdge {
    public final NetworkVertex left;
    public final NetworkVertex right;

    NetworkEdge(NetworkVertex left, NetworkVertex right) {
        this.left = left;
        this.right = right;
        left.edges.$plus$eq(this);
        right.edges.$plus$eq(this);
    }

    public NetworkVertex other(NetworkVertex side) {
        return side == left ? right : left;
    }

    public boolean isBetween(NetworkVertex a, NetworkVertex b) {
        return (a == left && b == right) || (b == left && a == right);
    }
}
