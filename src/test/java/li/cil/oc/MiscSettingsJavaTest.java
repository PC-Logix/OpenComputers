package li.cil.oc;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MiscSettingsJavaTest {
    @Test
    public void clampsGeneralLimitsAndKeepsConfiguredArrays() {
        MiscSettingsJava settings = new MiscSettingsJava(config());

        assertEquals(1, settings.maxScreenWidth);
        assertEquals(720, settings.maxScreenHeight);
        assertTrue(settings.inputUsername);
        assertEquals(0, settings.maxClipboardTextLength);
        assertEquals(5, settings.initialNetworkPacketTtl);
        assertEquals(0, settings.maxNetworkPacketSize);
        assertEquals(4, settings.maxNetworkPacketParts);
        assertEquals(0.0, settings.maxNetworkClientPacketDistance, 0);
        assertEquals(32.0, settings.maxNetworkClientEffectPacketDistance, 0);
        assertArrayEquals(new int[]{0, 2, 3}, settings.maxOpenPorts);
        assertArrayEquals(new double[]{0, 250}, settings.maxWirelessRange, 0);
        assertEquals(-5, settings.lootProbability);
        assertEquals(-10, settings.geolyzerRange);
        assertEquals(0.0f, settings.geolyzerNoise, 0);
        assertEquals(1.0, settings.disassemblerBreakChance, 0);
        assertEquals(0.0, settings.presentChance, 0);
        assertEquals(0, settings.dataCardSoftLimit);
        assertEquals(0, settings.dataCardHardLimit);
        assertEquals(0.0, settings.dataCardTimeout, 0);
        assertEquals(3, settings.serverRackSwitchTier);
        assertEquals(0.0, settings.redstoneDelay, 0);
        assertEquals(12.0, settings.tradingRange, 0);
        assertEquals(128, settings.mfuRange);
    }

    @Test
    public void restoresDefaultsForInvalidPortAndRangeArrays() {
        Config config = config()
            .withValue("misc.maxOpenPorts", ConfigValueFactory.fromAnyRef(List.of(99)))
            .withValue("misc.maxWirelessRange", ConfigValueFactory.fromAnyRef(List.of(99.0)));

        MiscSettingsJava settings = new MiscSettingsJava(config);

        assertArrayEquals(new int[]{16, 1, 16}, settings.maxOpenPorts);
        assertArrayEquals(new double[]{16, 400}, settings.maxWirelessRange, 0);
    }

    private static Config config() {
        return ConfigFactory.parseString("""
            misc {
              maxScreenWidth = 0
              maxScreenHeight = 720
              inputUsername = true
              maxClipboard = -1
              initialNetworkPacketTTL = 0
              maxNetworkPacketSize = -1
              maxNetworkPacketParts = 3
              maxNetworkClientPacketDistance = -4.0
              maxNetworkClientEffectPacketDistance = 32.0
              maxOpenPorts = [-1, 2, 3]
              maxWirelessRange = [-1.0, 250.0]
              updateCheck = false
              lootProbability = -5
              lootRecrafting = true
              geolyzerRange = -10
              geolyzerNoise = -1.25
              disassembleAllTheThings = false
              disassemblerBreakChance = 2.0
              disassemblerInputBlacklist = ["bad"]
              hideTier4 = true
              hideOpenScreens = false
              hideOwnSpecial = false
              allowItemStackInspection = true
              presentChance = -1.0
              assemblerBlacklist = ["blocked"]
              threadPriority = 5
              giveManualToNewPlayers = false
              dataCardSoftLimit = -1
              dataCardHardLimit = -2
              dataCardTimeout = -3.0
              serverRackSwitchTier = 99
              redstoneDelay = -1.0
              tradingRange = 12.0
              mfuRange = 129
            }
            """);
    }
}
