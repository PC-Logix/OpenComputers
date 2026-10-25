package li.cil.oc.common;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.junit.Test;
import scala.Option;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PacketParserTest {
    private final PacketHandlerBase handler = new PacketHandlerBase() {
        @Override protected Optional<Level> world(Player player, ResourceLocation dimension) { return Optional.empty(); }
        @Override public void dispatch(PacketParser parser) { }
        @Override protected PacketParser createParser(InputStream stream, Player player) { throw new UnsupportedOperationException(); }
    };

    @Test
    public void readsPacketHeaderAndMixedPayloadWithoutChangingFieldOrder() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(PacketType.Analyze.id());
            out.writeByte(-1);
            out.writeByte(Direction.NORTH.get3DDataValue());
            out.writeByte(0x56);
            out.writeByte(0x34);
            out.writeByte(0x12);
            out.writeInt(3);
            out.write(new byte[]{7, 8, 9});
            out.writeBoolean(false);
        }

        try (PacketHandlerBase.PacketParser parser = new PacketHandlerBase.PacketParser(new ByteArrayInputStream(bytes.toByteArray()), null, handler)) {
            assertSame(PacketType.Analyze, parser.packetType);
            assertTrue(parser.readDirectionOptional().isEmpty());
            assertSame(Direction.NORTH, parser.readDirection().get());
            assertEquals(0x123456, parser.readMedium());
            assertArrayEquals(new byte[]{7, 8, 9}, parser.readByteArray());
            assertEquals(null, parser.readNBT());
            assertEquals(-1, parser.read());
        }
    }
}
