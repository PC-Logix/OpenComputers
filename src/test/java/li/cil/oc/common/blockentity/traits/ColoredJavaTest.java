package li.cil.oc.common.blockentity.traits;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ColoredJavaTest {
    @Test
    public void colorStateReportsOnlyActualChanges() {
        ColoredJava.State state = new ColoredJava.State();
        assertEquals(0, state.color());
        assertFalse(state.setColor(0));
        assertTrue(state.setColor(0x123456));
        assertEquals(0x123456, state.color());
        assertFalse(state.setColor(0x123456));
        assertTrue(state.setColor(0));
        assertEquals(0, state.color());
    }
}
