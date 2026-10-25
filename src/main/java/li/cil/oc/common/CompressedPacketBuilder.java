package li.cil.oc.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class CompressedPacketBuilder extends PacketBuilderBase<DeflaterOutputStream> {
    private final PacketType packetType;
    private final ByteArrayOutputStream data;

    public CompressedPacketBuilder(PacketType packetType) {
        this(packetType, PacketBuilder.newData(true));
    }

    public static ByteArrayOutputStream $lessinit$greater$default$2() { return PacketBuilder.newData(true); }

    public CompressedPacketBuilder(PacketType packetType, ByteArrayOutputStream data) {
        super(new DeflaterOutputStream(data, new Deflater(Deflater.BEST_SPEED)));
        this.packetType = packetType;
        this.data = data;
        writeByteUnchecked(packetType.id());
    }

    private void writeByteUnchecked(int value) {
        try { writeByte(value); } catch (IOException exception) { throw new java.io.UncheckedIOException(exception); }
    }

    public PacketType packetType() { return packetType; }

    @Override
    protected byte[] packet() {
        try {
            flush();
            stream.finish();
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
        return data.toByteArray();
    }
}
