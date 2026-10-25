package li.cil.oc;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class SettingsFileJavaTest {
    @Test
    public void readsBundledDefaults() throws Exception {
        assertTrue(SettingsFileJava.loadDefaults().hasPath("opencomputers.version"));
    }

    @Test
    public void migratesSavedValuesAndFillsMissingDefaults() throws Exception {
        Path file = Files.createTempFile("oc-settings", ".conf");
        Files.writeString(file, "opencomputers { version = \"1.9.4\", value = \"saved\" }", StandardCharsets.UTF_8);
        Config defaults = ConfigFactory.parseString(
            "opencomputers { version = \"1.9.4\", value = \"default\", fallback = \"present\" }");

        Config parsed = SettingsFileJava.parseFile(file.toFile(), defaults);

        assertEquals("saved", parsed.getString("opencomputers.value"));
        assertEquals("present", parsed.getString("opencomputers.fallback"));
        Files.deleteIfExists(file);
    }

    @Test
    public void writesAReadableConfigurationFile() throws Exception {
        Path file = Files.createTempFile("oc-settings-write", ".conf");
        Config config = ConfigFactory.parseString("opencomputers { enabled = true, amount = 7 }");

        SettingsFileJava.saveConfig(file.toFile(), config);

        Config written = ConfigFactory.parseFile(file.toFile());
        assertTrue(written.getBoolean("opencomputers.enabled"));
        assertEquals(7, written.getInt("opencomputers.amount"));
        Files.deleteIfExists(file);
    }
}
