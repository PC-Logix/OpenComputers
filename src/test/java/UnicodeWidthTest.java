import li.cil.oc.util.FontUtils;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.TextBuffer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnicodeWidthTest {
    @Test
    public void classifiesZeroWidthSpaceAsZeroColumns() {
        assertEquals(0, FontUtils.wcwidth(0x200B));
    }

    @Test
    public void truncatesByDisplayWidthWithoutSplittingSupplementaryCodePoints() {
        assertEquals("A\u200B", FontUtils.wtrunc("A\u200BB", 2));
        assertEquals("\uD83D\uDE00", FontUtils.wtrunc("\uD83D\uDE00B", 3));
    }

    @Test
    public void doesNotAllocateATextBufferCellToZeroWidthSpace() {
        TextBuffer buffer = new TextBuffer(10, 1, new PackedColor.SingleBitFormat(0xFFFFFF));

        assertTrue(buffer.set(0, 0, "Mic\u200Bhiyo", false));
        assertEquals("Michiyo", buffer.lineToString(0).substring(0, 7));
        assertEquals(' ', buffer.get(7, 0));
    }
}
