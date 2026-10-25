package li.cil.oc.common.blockentity.traits;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class EnvironmentRuntimeStateJavaTest {
    @Test
    public void deferredChangeFlagStartsClearAndCanBeConsumed() {
        EnvironmentRuntimeStateJava state = new EnvironmentRuntimeStateJava();
        assertFalse(state.isChangeScheduled());

        state.setChangeScheduled(true);
        assertTrue(state.isChangeScheduled());
        state.setChangeScheduled(false);
        assertFalse(state.isChangeScheduled());
    }
}
