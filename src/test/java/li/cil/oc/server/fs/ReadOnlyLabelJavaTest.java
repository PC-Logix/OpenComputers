package li.cil.oc.server.fs;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class ReadOnlyLabelJavaTest {
    @Test
    public void labelValueIsReadableAndCannotBeChanged() {
        ReadOnlyLabelJava label = new ReadOnlyLabelJava("OpenOS");
        assertEquals("OpenOS", label.getLabel(null));
        try {
            label.setLabel("other");
            fail("read-only labels must reject changes");
        } catch (IllegalArgumentException expected) {
            assertEquals("label is read only", expected.getMessage());
        }
    }
}
