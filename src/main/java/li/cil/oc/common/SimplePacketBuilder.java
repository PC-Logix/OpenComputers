package li.cil.oc.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SimplePacketBuilder extends PacketBuilderBase<ByteArrayOutputStream> {
    private final PacketType packetType;

    public SimplePacketBuilder(PacketType packetType) {
        super(PacketBuilder.newData(false));
        this.packetType = packetType;
        writeByteUnchecked(packetType.id());
    }

    private void writeByteUnchecked(int value) {
        try { writeByte(value); } catch (IOException exception) { throw new java.io.UncheckedIOException(exception); }
    }

    public PacketType packetType() { return packetType; }

    @Override
    protected byte[] packet() {
        try { flush(); } catch (IOException exception) { throw new java.io.UncheckedIOException(exception); }
        return stream.toByteArray();
    }
}
