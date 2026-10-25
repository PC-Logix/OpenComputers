package li.cil.oc.common.blockentity.traits;

import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RedstoneAwareMapAccessJavaTest {
    @Test
    public void fuzzyLookupAcceptsIntegerAndEquivalentFloatingKeys() {
        assertEquals(Optional.of("integer"),
                RedstoneAwareJava.getObjectFuzzyOptional(Map.of(2, "integer", 3.0, "double"), 2));
        assertEquals(Optional.of("double"),
                RedstoneAwareJava.getObjectFuzzyOptional(Map.of(3.0, "double"), 3));
    }

    @Test
    public void fuzzyLookupTreatsNullAndMissingValuesAsEmpty() {
        assertFalse(RedstoneAwareJava.getObjectFuzzyOptional(Map.of(1, "placeholder"), 2).isPresent());
        Map<Integer, Object> nullable = new java.util.HashMap<>();
        nullable.put(1, null);
        assertFalse(RedstoneAwareJava.getObjectFuzzyOptional(nullable, 1).isPresent());
    }

    @Test
    public void numericScalaOptionsConvertToJavaOptionalIntegers() {
        assertEquals(Optional.of(17), RedstoneAwareJava.valueToIntOptional(scala.Option.apply(17.8)));
        assertFalse(RedstoneAwareJava.valueToIntOptional(scala.Option.empty()).isPresent());
        assertTrue(RedstoneAwareJava.valueToIntOptional("17").isEmpty());
    }
}
