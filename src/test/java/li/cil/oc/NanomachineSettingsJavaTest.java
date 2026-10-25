package li.cil.oc;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public final class NanomachineSettingsJavaTest {
    @Test
    public void clampsLimitsAndPreservesWhitelistValues() {
        var config = ConfigFactory.parseString("""
            nanomachines {
              triggerQuota = -1.0
              connectorQuota = 2.5
              maxInputs = 0
              maxOutputs = -1
              safeInputsActive = -2
              maxInputsActive = 3
              commandDelay = -0.5
              commandRange = 12.0
              magnetRange = -2.0
              disintegrationRange = -4
              potionWhitelist = ["minecraft:healing", "minecraft:swiftness"]
              hungryDamage = -1.0
              hungryEnergyRestored = 5.5
            }
            """);

        NanomachineSettingsJava settings = new NanomachineSettingsJava(config);

        assertEquals(0.0, settings.triggerQuota, 0);
        assertEquals(2.5, settings.connectorQuota, 0);
        assertEquals(1, settings.maxInputs);
        assertEquals(1, settings.maxOutputs);
        assertEquals(0, settings.safeInputsActive);
        assertEquals(3, settings.maxInputsActive);
        assertEquals(0.0, settings.commandDelay, 0);
        assertEquals(12.0, settings.commandRange, 0);
        assertEquals(0.0, settings.magnetRange, 0);
        assertEquals(0, settings.disintegrationRange);
        assertEquals(List.of("minecraft:healing", "minecraft:swiftness"), settings.potionWhitelist);
        assertEquals(0.0f, settings.hungryDamage, 0);
        assertEquals(5.5, settings.hungryEnergyRestored, 0);
    }
}
