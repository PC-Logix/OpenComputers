package li.cil.oc.util;

import org.junit.Test;
import scala.Option;

import java.util.Optional;
import java.util.function.IntSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public final class InventoryAccessJavaTest {
    @Test
    public void scalaLookupResultsAdaptToJavaOptional() {
        assertEquals(Optional.of("found"), InventoryAccessJava.toOptional(Option.apply("found")));
        assertFalse(InventoryAccessJava.toOptional(Option.empty()).isPresent());
    }

    @Test
    public void scalaTransferCallbackAdaptsToAJavaIntSupplier() {
        IntSupplier callback = InventoryAccessJava.toIntSupplier(() -> 23);
        assertEquals(23, callback.getAsInt());
        assertNull(InventoryAccessJava.toIntSupplier(null));
    }
}
