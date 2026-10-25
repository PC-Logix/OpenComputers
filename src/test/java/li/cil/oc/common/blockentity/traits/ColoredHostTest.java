package li.cil.oc.common.blockentity.traits;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ColoredHostTest {
    @Test
    public void javaHostsReceiveOneNotificationForEachActualColorChange() {
        Host first = new Host();
        Host second = new Host();
        first.setColor(0);
        first.setColor(0x123456);
        first.setColor(0x123456);
        assertEquals(0x123456, first.getColor());
        assertEquals(1, first.notifications);
        assertEquals(0, second.getColor());
        assertFalse(first.controlsConnectivity());
    }

    private static final class Host implements Colored {
        private final ColoredJava.State state = new ColoredJava.State();
        private int notifications;

        @Override public ColoredJava.State colorState() { return state; }
        @Override public void onColorChanged() { notifications++; }
    }
}
