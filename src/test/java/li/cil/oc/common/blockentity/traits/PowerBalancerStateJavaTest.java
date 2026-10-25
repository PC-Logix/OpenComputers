package li.cil.oc.common.blockentity.traits;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class PowerBalancerStateJavaTest {
    @Test
    public void networkEnergySnapshotDefaultsToZeroAndStoresValuesSeparately() {
        PowerBalancerStateJava state = new PowerBalancerStateJava();
        assertEquals(0.0, state.globalBuffer(), 0.0);
        assertEquals(0.0, state.globalBufferSize(), 0.0);

        state.setGlobalBuffer(12.5);
        state.setGlobalBufferSize(64.0);
        assertEquals(12.5, state.globalBuffer(), 0.0);
        assertEquals(64.0, state.globalBufferSize(), 0.0);
    }
}
