package li.cil.oc.common.blockentity.traits;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public final class HubRuntimeStateJavaTest {
    @Test
    public void ownsQueueTuningCooldownAndCycleAverage() {
        HubRuntimeStateJava state = new HubRuntimeStateJava(32, 4, 8);
        assertEquals(32, state.maxQueueSize());
        assertEquals(4, state.relayDelay());
        assertEquals(8, state.relayAmount());
        assertEquals(-1, state.relayCooldown());
        assertEquals(20, state.packetsPerCycleAverage().size());

        state.setMaxQueueSize(64);
        state.setRelayDelay(2);
        state.setRelayAmount(3);
        state.setRelayCooldown(1);
        assertEquals(64, state.maxQueueSize());
        assertEquals(2, state.relayDelay());
        assertEquals(3, state.relayAmount());
        assertEquals(1, state.relayCooldown());
        assertSame(state.queue(), state.queue());
        assertSame(state.packetsPerCycleAverage(), state.packetsPerCycleAverage());
    }
}
