import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.PackedColorColor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PackedColorTest {
    @Test
    public void paletteFormatPreservesPaletteIndexesAndExactColors() {
        PackedColor.MutablePaletteFormat format = new PackedColor.MutablePaletteFormat();

        assertEquals(0, format.deflate(new PackedColorColor(0xFFFFFF)));
        assertEquals(0xFFFFFF, format.inflate(0));
        format.update(2, 0x123456);
        assertEquals(0x123456, format.inflate(2));
        assertTrue(format.isFromPalette(2));
    }

    @Test
    public void hybridFormatUsesEightBitColorCube() {
        PackedColor.HybridFormat format = new PackedColor.HybridFormat();

        short encoded = format.deflate(new PackedColorColor(0xFF0000));

        assertEquals(TextBuffer.ColorDepth.EightBit, format.depth());
        assertEquals(216, encoded & 0xFF);
        assertEquals(0xFF0000, format.inflate(encoded & 0xFF));
        assertTrue(format.isFromPalette(0));
        assertTrue(!format.isFromPalette(16));
    }

    @Test
    public void sixteenBitFormatRoundTripsRgb565PrimaryColors() {
        PackedColor.SixteenBitFormat format = new PackedColor.SixteenBitFormat();
        int red = format.deflate(new PackedColorColor(0xFF0000)) & 0xFFFF;

        assertEquals(TextBuffer.ColorDepth.SixteenBit, format.depth());
        assertEquals(0xFF0000, format.inflate(red));
    }
}
