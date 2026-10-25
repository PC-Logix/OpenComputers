package li.cil.oc.client.audio;

import li.cil.oc.util.DFPWM;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;
import org.lwjgl.openal.AL10;
import java.util.*;
import static org.junit.Assert.*;

public final class AudioSessionTest {
    @Test public void dfpwmPacketsRetainDecoderStateAndLegacyBufferCadence() throws Exception {
        byte[] encoded = new byte[4096];
        for (int i = 0; i < encoded.length; i++) encoded[i] = (byte) (i * 73 + (i >>> 3) * 19);
        byte[] expected = new byte[32768]; new DFPWM().decompress(expected, encoded, 0, 0, encoded.length);
        for (int i = 0; i < expected.length; i++) expected[i] ^= (byte) 0x80;
        var session = new AudioSession(1, 0, 48000, 1, AL10.AL_FORMAT_MONO8, Vec3.ZERO, true, 1);
        for (int offset = 0; offset < encoded.length; offset += 1024) session.append(Arrays.copyOfRange(encoded, offset, offset + 1024));
        var buffers = pending(session);
        assertEquals(4, buffers.size());
        int offset = 0;
        for (byte[] buffer : buffers) {
            assertEquals(8192, buffer.length);
            assertArrayEquals(Arrays.copyOfRange(expected, offset, offset + buffer.length), buffer);
            offset += buffer.length;
        }
    }
    @Test public void pcmSplitsOnlyAtOpenAlBoundaryAndKeepsFinalPartialBuffer() throws Exception {
        byte[] pcm = new byte[17000]; new Random(123).nextBytes(pcm);
        var session = new AudioSession(2, 0, 32768, 1, AL10.AL_FORMAT_MONO8, Vec3.ZERO, false, 1);
        session.append(pcm);
        var buffers = new ArrayList<>(pending(session));
        assertEquals(3, buffers.size());
        assertArrayEquals(Arrays.copyOfRange(pcm, 0, 8192), buffers.get(0));
        assertArrayEquals(Arrays.copyOfRange(pcm, 8192, 16384), buffers.get(1));
        assertArrayEquals(Arrays.copyOfRange(pcm, 16384, 17000), buffers.get(2));
        pcm[0] ^= 1;
        assertNotEquals(pcm[0], buffers.get(0)[0]);
    }
    @SuppressWarnings("unchecked") private static Queue<byte[]> pending(AudioSession session) throws Exception {
        var field = AudioSession.class.getDeclaredField("pendingBuffers"); field.setAccessible(true);
        return (Queue<byte[]>) field.get(session);
    }
}
