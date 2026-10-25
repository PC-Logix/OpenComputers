package li.cil.oc.common.component;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class TextBufferPowerJavaTest {
    @Test
    public void absentPowerTransitionDoesNotProducePacketState() {
        assertNull(TextBufferPowerJava.reportedPowerState(true, null));
    }

    @Test
    public void powerTransitionIsMaskedWhenDisplayIsOff() {
        assertEquals(Boolean.FALSE, TextBufferPowerJava.reportedPowerState(false, true));
    }

    @Test
    public void activeDisplayReportsPowerTransition() {
        assertEquals(Boolean.TRUE, TextBufferPowerJava.reportedPowerState(true, true));
        assertEquals(Boolean.FALSE, TextBufferPowerJava.reportedPowerState(true, false));
    }
}
