package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.Direction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class OpenSidesJavaTest {
    @Test
    public void initializesEachSideUsingTheDefaultCallback() {
        int[] calls = {0};
        boolean[] sides = OpenSidesJava.initialize(Direction.values().length, () -> {
            calls[0]++;
            return true;
        });

        assertEquals(Direction.values().length, calls[0]);
        assertEquals(Direction.values().length, sides.length);
        for (boolean side : sides) assertTrue(side);
    }

    @Test
    public void sideMutationsAndCompressionRoundTrip() {
        boolean[] sides = OpenSidesJava.initialize(Direction.values().length, () -> false);
        assertTrue(OpenSidesJava.setOpen(Direction.NORTH, true, sides));
        assertTrue(OpenSidesJava.isOpen(Direction.NORTH, sides));
        assertFalse(OpenSidesJava.setOpen(null, true, sides));

        byte encoded = OpenSidesJava.compress(sides);
        assertTrue(OpenSidesJava.isOpen(Direction.NORTH, OpenSidesJava.uncompress(encoded)));
        assertFalse(OpenSidesJava.isOpen(Direction.SOUTH, OpenSidesJava.uncompress(encoded)));
    }

    @Test
    public void stateKeepsTheSameArrayUntilExplicitlyReplaced() {
        OpenSidesJava.State state = new OpenSidesJava.State(Direction.values().length, () -> false);
        boolean[] initial = state.get();
        assertTrue(initial == state.get());

        boolean[] replacement = OpenSidesJava.uncompress((byte) 1);
        state.set(replacement);
        assertTrue(replacement == state.get());
    }
}
