package li.cil.oc.util;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WavePcmTest {
    @Test public void squareWavePreservesUnsignedSamplesAndDelay() {
        byte[] pcm = WavePcm.generate(8000, 32, 0, 1000, 50, 10, 0, 0, 0, 0, 0, 1, 0);
        assertEquals(480, pcm.length);
        for (int i = 0; i < 80; i++) assertEquals(127, pcm[i] & 255);
        for (int i = 0; i < 8; i++) assertEquals(i < 4 ? 159 : 95, pcm[80 + i] & 255);
    }
    @Test public void attackAndReleaseEnvelopeKeepsSilenceAtAttackStart() {
        byte[] pcm = WavePcm.generate(8000, 32, 0, 1000, 50, 0, 0, 0, 0, 10, 0, 1, 10);
        assertEquals(127, pcm[0] & 255);
        assertTrue(Math.abs((pcm[1] & 255) - 127) < 2);
        assertTrue(Math.abs((pcm[399] & 255) - 127) < 2);
        assertEquals(159, pcm[160] & 255);
    }
    @Test public void durationAndDelayAreBoundedBeforeAllocatingPcm() {
        assertEquals(400, WavePcm.generate(8000, 32, 1, 440, -1, -1, 0, 0, 0, 0, 0, 1, 0).length);
        assertEquals(168000, WavePcm.generate(8000, 32, 1, 440, 99999, 99999, 0, 0, 0, 0, 0, 1, 0).length);
    }
}
