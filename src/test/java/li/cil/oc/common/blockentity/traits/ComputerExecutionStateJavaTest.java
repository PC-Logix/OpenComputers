package li.cil.oc.common.blockentity.traits;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ComputerExecutionStateJavaTest {
    @Test
    public void requestedStartClearsErrorButStoppingPreservesIt() {
        ComputerExecutionStateJava state = new ComputerExecutionStateJava();
        state.setHasErrored(true);
        state.requestRunning(true);
        assertTrue(state.isRunning());
        assertFalse(state.hasErrored());

        state.setHasErrored(true);
        state.requestRunning(false);
        assertFalse(state.isRunning());
        assertTrue(state.hasErrored());
    }

    @Test
    public void observedMachineStateReplacesBothFlags() {
        ComputerExecutionStateJava state = new ComputerExecutionStateJava();
        state.requestRunning(true);
        state.observe(false, true);
        assertFalse(state.isRunning());
        assertTrue(state.hasErrored());
    }
}
