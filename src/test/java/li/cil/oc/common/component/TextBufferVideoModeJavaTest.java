package li.cil.oc.common.component;

import org.junit.Test;
import scala.Tuple2;

import static org.junit.Assert.assertEquals;

public final class TextBufferVideoModeJavaTest {
    @Test
    public void maximumResolutionRequiresPositiveDimensions() {
        TextBufferVideoModeJava.validateMaximumResolution(1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void maximumResolutionRejectsZeroWidth() {
        TextBufferVideoModeJava.validateMaximumResolution(0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void maximumResolutionRejectsNegativeHeight() {
        TextBufferVideoModeJava.validateMaximumResolution(1, -1);
    }

    @Test
    public void aspectRatioUsesWidthOverHeight() {
        assertEquals(1.5, TextBufferVideoModeJava.aspectRatio(new Tuple2<>(3.0, 2.0)), 0.0);
    }
}
