package li.cil.oc.common.component;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class TextBufferUpdateJavaTest {
    @Test
    public void snapshotIsRequestedOnlyOnClientBeforeInitialization() {
        assertTrue(TextBufferUpdateJava.shouldRequestSynchronization(true, false));
        assertFalse(TextBufferUpdateJava.shouldRequestSynchronization(true, true));
        assertFalse(TextBufferUpdateJava.shouldRequestSynchronization(false, false));
    }
}
