package li.cil.oc.common.blockentity.traits;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public final class RedstoneStateJavaTest {
    @Test
    public void directionalArraysKeepTheirDefaultValuesAndStableIdentity() {
        RedstoneStateJava state = new RedstoneStateJava();
        assertEquals(6, state.input().length);
        assertEquals(6, state.output().length);
        for (int side = 0; side < 6; side++) {
            assertEquals(-1, state.input()[side]);
            assertEquals(0, state.output()[side]);
        }

        state.output()[2] = 15;
        assertEquals(15, state.output()[2]);
        assertSame(state.input(), state.input());
        assertSame(state.output(), state.output());
    }

    @Test
    public void runtimeFlagsKeepTheirDefaultsAndCanBeUpdatedIndependently() {
        RedstoneStateJava state = new RedstoneStateJava();
        assertEquals(false, state.outputEnabled());
        assertEquals(true, state.shouldUpdateInput());

        state.outputEnabled(true);
        state.shouldUpdateInput(false);
        assertEquals(true, state.outputEnabled());
        assertEquals(false, state.shouldUpdateInput());
    }
}
