package li.cil.oc.util;

import net.minecraft.core.Direction;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class InventoryUtilsOptionalTest {
    @Test
    public void optionalDirectionBridgesPresentAndEmptyValues() {
        scala.Option<Direction> present = InventoryUtils.toScalaOption(Optional.of(Direction.WEST));
        assertTrue(present.isDefined());
        assertEquals(Direction.WEST, present.get());

        assertFalse(InventoryUtils.toScalaOption(Optional.empty()).isDefined());
    }
}
