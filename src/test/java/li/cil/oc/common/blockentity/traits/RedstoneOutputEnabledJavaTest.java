package li.cil.oc.common.blockentity.traits;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public final class RedstoneOutputEnabledJavaTest {
    @Test
    public void onlyTransitionsNotifyAndDisablingClearsOutputs() {
        int[] output = {1, 0, 7, 0, 15, 3};
        AtomicBoolean enabled = new AtomicBoolean(true);
        int[] notifications = {0};

        RedstoneAwareJava.setOutputEnabled(true, true, output, enabled::set, () -> notifications[0]++);
        assertEquals(0, notifications[0]);
        assertArrayEquals(new int[]{1, 0, 7, 0, 15, 3}, output);

        RedstoneAwareJava.setOutputEnabled(true, false, output, enabled::set, () -> notifications[0]++);
        assertEquals(false, enabled.get());
        assertEquals(1, notifications[0]);
        assertArrayEquals(new int[6], output);

        RedstoneAwareJava.setOutputEnabled(false, false, output, enabled::set, () -> notifications[0]++);
        assertEquals(1, notifications[0]);
    }
}
