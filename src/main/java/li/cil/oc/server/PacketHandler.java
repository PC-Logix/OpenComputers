package li.cil.oc.server;

/** Singleton server-side packet parser registered with the active packet dispatcher. */
public final class PacketHandler extends ServerPacketHandler {
    public static final PacketHandler INSTANCE = new PacketHandler();

    private PacketHandler() {}
}