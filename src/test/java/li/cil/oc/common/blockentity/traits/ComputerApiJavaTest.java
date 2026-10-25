package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.util.StateAware;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ComputerApiJavaTest {
    @Test
    public void currentStateReflectsWhetherTheComputerIsRunning() {
        assertEquals(EnumSet.of(StateAware.State.IsWorking), ComputerApiJava.currentState(true));
        assertTrue(ComputerApiJava.currentState(false).isEmpty());
    }
}
