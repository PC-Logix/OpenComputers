package li.cil.oc;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class PrinterSettingsJavaTest {
    @Test
    public void preservesUnboundedValuesAndClampsConfiguredLimits() {
        var config = ConfigFactory.parseString("""
            printer {
              maxShapes = -2
              recycleRate = -0.5
              chameliumEdible = true
              maxBaseLightLevel = 20
              customRedstoneCost = -1
              materialValue = 10
              inkValue = -3
              printsHaveOpacity = true
              noclipMultiplier = -4.0
            }
            """);

        PrinterSettingsJava settings = new PrinterSettingsJava(config);

        assertEquals(-2, settings.maxComplexity);
        assertEquals(-0.5, settings.recycleRate, 0);
        assertTrue(settings.chameliumEdible);
        assertEquals(15, settings.maxLightLevel);
        assertEquals(0, settings.customRedstoneCost);
        assertEquals(10, settings.materialValue);
        assertEquals(0, settings.inkValue);
        assertTrue(settings.printsHaveOpacity);
        assertEquals(0.0, settings.noclipMultiplier, 0);
    }
}
