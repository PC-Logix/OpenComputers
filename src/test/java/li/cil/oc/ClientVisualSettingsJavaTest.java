package li.cil.oc;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ClientVisualSettingsJavaTest {
    @Test
    public void clampsVisualRangesAndPreservesConfiguredValues() {
        ClientVisualSettingsJava settings = new ClientVisualSettingsJava(config());

        assertEquals(80.0, settings.screenTextFadeStartDistance, 0);
        assertEquals(160.0, settings.maxScreenTextRenderDistance, 0);
        assertTrue(settings.textLinearFiltering);
        assertTrue(settings.textAntiAlias);
        assertTrue(settings.robotLabels);
        assertEquals(0.5, settings.fontCharScale, 0);
        assertEquals(0.0, settings.hologramFadeStartDistance, 0);
        assertEquals(128.0, settings.hologramRenderDistance, 0);
        assertEquals(0.0, settings.hologramFlickerFrequency, 0);
        assertEquals(0xFF00FF, settings.monochromeColor);
        assertEquals("default", settings.fontRenderer);
        assertArrayEquals(new double[]{0.2, 0.8}, settings.nanomachineHudPosition, 0);
        assertTrue(settings.enableNanomachinePfx);
        assertEquals(64, settings.transposerFluidTransferRate);
    }

    @Test
    public void usesDefaultHudPositionForInvalidCoordinates() {
        Config config = config().withValue("client.nanomachineHudPos", ConfigValueFactory.fromAnyRef(java.util.List.of(0.5)));
        ClientVisualSettingsJava settings = new ClientVisualSettingsJava(config);
        assertArrayEquals(new double[]{-1.0, -1.0}, settings.nanomachineHudPosition, 0);
    }

    private static Config config() {
        return ConfigFactory.parseString("""
            client {
              screenTextFadeStartDistance = 80.0
              maxScreenTextRenderDistance = 160.0
              textLinearFiltering = true
              textAntiAlias = true
              robotLabels = true
              fontCharScale = -1.0
              hologramFadeStartDistance = -1.0
              hologramRenderDistance = 128.0
              hologramFlickerFrequency = -2.0
              monochromeColor = "0xFF00FF"
              fontRenderer = "default"
              nanomachineHudPos = [0.2, 0.8]
              enableNanomachinePfx = true
            }
            misc.transposerFluidTransferRate = 64
            """);
    }
}
