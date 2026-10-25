package li.cil.oc.common.blockentity.traits;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RedstoneInputSchedulingTest {
    @Test
    public void samplesPendingInputsOnlyOnServerTick() {
        assertTrue(RedstoneAwareJava.shouldUpdateAfterTick(true, true));
        assertFalse(RedstoneAwareJava.shouldUpdateAfterTick(false, true));
        assertFalse(RedstoneAwareJava.shouldUpdateAfterTick(true, false));
    }

    @Test
    public void onlySchedulesInitialSamplingForNonTickingServerHosts() {
        assertTrue(RedstoneAwareJava.shouldScheduleAfterRemoval(false, true));
        assertFalse(RedstoneAwareJava.shouldScheduleAfterRemoval(true, true));
        assertFalse(RedstoneAwareJava.shouldScheduleAfterRemoval(false, false));
    }
}
