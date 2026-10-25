package li.cil.oc;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class IntegrationSettingsJavaTest {
    @Test
    public void preservesProfilesListsAndIntegrationOptions() {
        var config = ConfigFactory.parseString("""
            integration {
              modBlacklist = ["examplemod"]
              peripheralBlacklist = ["examplemod:device"]
              fakePlayerUuid = "00000000-0000-0000-0000-000000000001"
              fakePlayerName = "OC Fake Player"
              vanilla {
                enableInventoryDriver = true
                enableTankDriver = false
                enableCommandBlockDriver = true
                allowItemStackNBTTags = false
              }
              buildcraft.programmingTableCost = -2.0
            }
            """);

        IntegrationSettingsJava settings = new IntegrationSettingsJava(config);

        assertEquals(List.of("examplemod"), settings.modBlacklist);
        assertEquals(List.of("examplemod:device"), settings.peripheralBlacklist);
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), settings.fakePlayerProfile.getId());
        assertEquals("OC Fake Player", settings.fakePlayerProfile.getName());
        assertTrue(settings.enableInventoryDriver);
        assertFalse(settings.enableTankDriver);
        assertTrue(settings.enableCommandBlockDriver);
        assertFalse(settings.allowItemStackNbtTags);
        assertEquals(0.0, settings.costProgrammingTable, 0);
    }
}
