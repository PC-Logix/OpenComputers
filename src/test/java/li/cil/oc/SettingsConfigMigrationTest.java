package li.cil.oc;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public final class SettingsConfigMigrationTest {
    @Test
    public void migratesLegacyInternetListsInTheirOriginalOrder() {
        Config oldConfig = ConfigFactory.parseString("""
            opencomputers {
              version = "1.8.2"
              internet {
                blacklist = ["10.0.0.0/8", "blocked.example"]
                whitelist = ["allowed.example"]
              }
            }
            """);
        Config defaults = ConfigFactory.parseString("""
            opencomputers {
              version = "1.9.4"
              internet.filteringRules = ["deny private", "deny bogon", "allow all"]
            }
            """);

        Config migrated = SettingsConfigMigration.patch(oldConfig, defaults);

        assertEquals("1.9.4", migrated.getString("opencomputers.version"));
        assertEquals(List.of(
            "deny ip:10.0.0.0/8",
            "deny domain:blocked.example",
            "allow domain:allowed.example",
            "deny all",
            "deny private",
            "deny bogon",
            "allow all"), migrated.getStringList("opencomputers.internet.filteringRules"));
        assertEquals(List.of(), migrated.getStringList("opencomputers.internet.blacklist"));
        assertEquals(List.of(), migrated.getStringList("opencomputers.internet.whitelist"));
    }

    @Test
    public void resetsSettingsCoveredByVersionedBehaviorChanges() {
        Config oldConfig = ConfigFactory.parseString("""
            opencomputers {
              version = "1.5.0"
              switch.relayDelayUpgrade = 9.0
              misc.keepThisValue = 17
            }
            """);
        Config defaults = ConfigFactory.parseString("""
            opencomputers {
              version = "1.9.4"
              switch.relayDelayUpgrade = 0.25
            }
            """);

        Config migrated = SettingsConfigMigration.patch(oldConfig, defaults);

        assertEquals(0.25, migrated.getDouble("opencomputers.switch.relayDelayUpgrade"), 0);
        assertEquals(17, migrated.getInt("opencomputers.misc.keepThisValue"));
    }
}
