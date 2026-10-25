package li.cil.oc;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public final class GpuSettingsJavaTest {
    @Test
    public void keepsConfiguredMemoryCostsAndResolution() {
        var config = ConfigFactory.parseString("""
            gpu {
              vramSizes = [1.0, 2.0, 3.0, 4.0]
              bitbltCost = 0.75
              defaultResolution = [80, 25]
            }
            """);

        GpuSettingsJava settings = new GpuSettingsJava(config);

        assertArrayEquals(new double[]{1, 2, 3, 4}, settings.vramSizes, 0);
        assertEquals(0.75, settings.bitbltCost, 0);
        assertArrayEquals(new int[]{80, 25}, settings.defaultResolution);
    }

    @Test
    public void restoresDefaultsForInvalidMemoryAndResolutionValues() {
        var config = ConfigFactory.parseString("""
            gpu {
              vramSizes = [1.0]
              defaultResolution = [-1, 25]
            }
            """);

        GpuSettingsJava settings = new GpuSettingsJava(config);

        assertArrayEquals(new double[]{1, 2, 3, 4}, settings.vramSizes, 0);
        assertEquals(0.5, settings.bitbltCost, 0);
        assertArrayEquals(new int[]{0, 0}, settings.defaultResolution);
    }
}
