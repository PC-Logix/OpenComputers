package li.cil.oc;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class SettingsConfigValidationTest {
    @Test
    public void acceptsMissingAndEmptyRemovedLists() {
        SettingsConfigValidation.validateRemovedListOptions(ConfigFactory.parseString("""
            opencomputers.internet.blacklist = []
            """));
        SettingsConfigValidation.validateRemovedListOptions(ConfigFactory.parseString(""));
    }

    @Test
    public void rejectsNonEmptyRemovedListsWithTheLegacyMessage() {
        try {
            SettingsConfigValidation.validateRemovedListOptions(ConfigFactory.parseString("""
                opencomputers.internet.whitelist = ["legacy.example"]
                """));
            fail("expected removed option validation to fail");
        } catch (RuntimeException error) {
            assertEquals("Error parsing configuration file: removed configuration option 'internet.whitelist' is not empty. This option should no longer be used.",
                error.getMessage());
        }
    }
}
