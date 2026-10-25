package li.cil.oc.server.fs;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class InputStreamChannelTest {
    @Test
    public void readAdvancesTheDestinationBufferAndChannelPosition() throws IOException {
        InputStreamChannel channel = new InputStreamChannel(new ByteArrayInputStream(new byte[]{10, 20, 30, 40}));
        ByteBuffer destination = ByteBuffer.allocate(4);
        destination.position(1);
        destination.limit(3);

        assertEquals(2, channel.read(destination));
        assertEquals(3, destination.position());
        assertArrayEquals(new byte[]{0, 10, 20, 0}, destination.array());
        assertEquals(2, channel.position());

        assertEquals(1, channel.position(1));
        byte[] next = new byte[1];
        assertEquals(1, channel.read(next));
        assertArrayEquals(new byte[]{20}, next);
    }

    @Test
    public void closeMarksTheChannelClosed() {
        InputStreamChannel channel = new InputStreamChannel(new ByteArrayInputStream(new byte[]{1}));
        assertTrue(channel.isOpen());

        channel.close();

        assertFalse(channel.isOpen());
    }
}
