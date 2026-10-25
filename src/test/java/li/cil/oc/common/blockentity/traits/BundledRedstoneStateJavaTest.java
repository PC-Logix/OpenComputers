package li.cil.oc.common.blockentity.traits;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public final class BundledRedstoneStateJavaTest {
    @Test
    public void arraysRetainTheirSixSidesSixteenColorsAndInitialValues() {
        BundledRedstoneStateJava state = new BundledRedstoneStateJava();
        assertEquals(6, state.bundledInput().length);
        assertEquals(16, state.bundledInput()[0].length);
        assertEquals(-1, state.bundledInput()[5][15]);
        assertEquals(-1, state.rednetInput()[0][0]);
        assertEquals(0, state.bundledOutput()[3][8]);

        state.bundledOutput()[3][8] = 15;
        assertEquals(15, state.bundledOutput()[3][8]);
        assertSame(state.bundledInput(), state.bundledInput());
    }
}
