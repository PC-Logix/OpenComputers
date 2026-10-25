package li.cil.oc.server.network;

import li.cil.oc.api.network.Packet;
import li.cil.oc.Settings;
import net.minecraft.nbt.CompoundTag;

/** Public packet creation and restoration rules for the server network API. */
public final class NetworkPacketFactory {
    private NetworkPacketFactory() {}

    public static Packet create(String source, String destination, int port, Object[] data,
                                int defaultTtl, int maxPacketSize) {
        Packet packet = new NetworkPacket(source, destination, port, data, defaultTtl,
            Settings.get().maxNetworkPacketParts());
        // Keep the size limit out of packet construction so saved packets can be restored.
        if (packet.size() > maxPacketSize) {
            throw new IllegalArgumentException("packet too big (max " + maxPacketSize + ")");
        }
        return packet;
    }

    public static Packet load(CompoundTag nbt) {
        String source = nbt.getString("source");
        String destination = nbt.contains("dest") ? null : nbt.getString("dest");
        int port = nbt.getInt("port");
        int ttl = nbt.getInt("ttl");
        Object[] data = NetworkPacketData.loadPayload(nbt);
        return new NetworkPacket(source, destination, port, data, ttl,
            Settings.get().maxNetworkPacketParts());
    }
}
