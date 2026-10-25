package li.cil.oc.common;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

import static org.junit.Assert.assertArrayEquals;

public class PacketBuilderTest {
    @Test
    public void simplePacketKeepsItsHeaderAndBigEndianPayload() throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.Analyze);
        packet.writeInt(0x01020304);

        assertArrayEquals(new byte[]{0, (byte) PacketType.Analyze.id(), 1, 2, 3, 4}, packet.packet());
    }

    @Test
    public void compressedPacketKeepsItsMarkerAndPayload() throws IOException {
        CompressedPacketBuilder packet = new CompressedPacketBuilder(PacketType.Analyze);
        packet.writeInt(0x01020304);
        byte[] bytes = packet.packet();

        assertArrayEquals(new byte[]{1}, new byte[]{bytes[0]});
        try (InflaterInputStream inflated = new InflaterInputStream(new ByteArrayInputStream(bytes, 1, bytes.length - 1))) {
            assertArrayEquals(new byte[]{(byte) PacketType.Analyze.id(), 1, 2, 3, 4}, inflated.readAllBytes());
        }
    }
}
