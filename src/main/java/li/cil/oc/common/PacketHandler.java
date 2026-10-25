package li.cil.oc.common;

/** Active packet parsers for each logical side. */
public final class PacketHandler {
    public static PacketHandlerBase clientHandler;
    public static PacketHandlerBase serverHandler;

    private PacketHandler() {}
}
