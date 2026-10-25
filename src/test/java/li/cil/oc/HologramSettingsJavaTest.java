package li.cil.oc;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class HologramSettingsJavaTest {
    @Test
    public void clampsConfiguredTierBoundsAndDelay() {
        Config config = ConfigFactory.parseString("""
            hologram {
              maxScale = [0.5, 2.0, 6.0]
              maxTranslation = [-1.0, 0.25, 1.0]
              setRawDelay = -2.0
              emitLight = true
            }
            """);

        HologramSettingsJava settings = new HologramSettingsJava(config);

        assertArrayEquals(new double[]{1.0, 2.0, 6.0}, settings.maxScaleByTier, 0);
        assertArrayEquals(new double[]{0.0, 0.25, 1.0}, settings.maxTranslationByTier, 0);
        assertEquals(0.0, settings.setRawDelay, 0);
        assertTrue(settings.emitLight);
    }

    @Test
    public void usesDefaultsForInvalidTierArrays() {
        Config config = ConfigFactory.parseString("""
            hologram {
              maxScale = [1.0]
              maxTranslation = [1.0]
              setRawDelay = 0.0
              emitLight = false
            }
            """);

        HologramSettingsJava settings = new HologramSettingsJava(config);

        assertArrayEquals(new double[]{3.0, 4.0, 5.0}, settings.maxScaleByTier, 0);
        assertArrayEquals(new double[]{0.25, 0.5, 0.75}, settings.maxTranslationByTier, 0);
    }
}
