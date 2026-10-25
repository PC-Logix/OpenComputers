package li.cil.oc.server.network;

import li.cil.oc.api.network.Packet;
import net.minecraft.nbt.CompoundTag;

/** Java implementation shared by the server's network packet wrapper. */
class NetworkPacket implements Packet {
    private String source;
    private String destination;
    private int port;
    private Object[] data;
    private int ttl;
    private final int size;
    private final int maxParts;

    NetworkPacket(String source, String destination, int port, Object[] data, int ttl, int maxParts) {
        this.source = source;
        this.destination = destination;
        this.port = port;
        this.data = data;
        this.ttl = ttl;
        this.maxParts = maxParts;
        this.size = NetworkPacketData.size(data, maxParts);
    }

    @Override public String source() { return source; }
    public void source_$eq(String source) { this.source = source; }
    @Override public String destination() { return destination; }
    public void destination_$eq(String destination) { this.destination = destination; }
    @Override public int port() { return port; }
    public void port_$eq(int port) { this.port = port; }
    @Override public Object[] data() { return data; }
    public void data_$eq(Object[] data) { this.data = data; }
    @Override public int size() { return size; }
    @Override public int ttl() { return ttl; }
    public void ttl_$eq(int ttl) { this.ttl = ttl; }

    @Override
    public Packet hop() {
        return new NetworkPacket(source, destination, port, data, ttl - 1, maxParts);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        NetworkPacketData.saveData(source, destination, port, data, ttl, nbt);
    }

    @Override
    public String toString() {
        return "{source = " + source + ", destination = " + destination + ", port = " + port
                + ", data = [" + java.util.Arrays.stream(data).map(String::valueOf).collect(java.util.stream.Collectors.joining(", ")) + "]}";
    }
}
