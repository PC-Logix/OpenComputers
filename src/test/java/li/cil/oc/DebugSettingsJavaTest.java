package li.cil.oc;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class DebugSettingsJavaTest {
    @Test
    public void preservesRuntimeOptionsAndDisableFlagInversion() {
        var config = ConfigFactory.parseString("""
            debug {
              logCallbackErrors = true
              forceLuaJ = false
              disableUserdata = true
              disablePersistence = false
              disableMemoryLimit = true
              forceCaseInsensitiveFS = true
              logFullNativeLibLoadErrors = false
              forceNativeLibPlatform = "windows-x86_64"
              forceNativeLibPathFirst = "native"
              logOpenGLErrors = true
              logHexFontErrors = false
              alwaysTryNative = true
              verbosePersistenceErrors = true
              nativeInTmpDir = false
              periodicallyForceLightUpdate = true
              insertIdsInConverters = false
              registerLuaJArchitecture = true
              disableLocaleChanging = true
            }
            """);

        DebugSettingsJava settings = new DebugSettingsJava(config);

        assertTrue(settings.logLuaCallbackErrors);
        assertFalse(settings.forceLuaJ);
        assertFalse(settings.allowUserdata);
        assertTrue(settings.allowPersistence);
        assertFalse(settings.limitMemory);
        assertTrue(settings.forceCaseInsensitive);
        assertFalse(settings.logFullLibLoadErrors);
        assertEquals("windows-x86_64", settings.forceNativeLibPlatform);
        assertEquals("native", settings.forceNativeLibPathFirst);
        assertTrue(settings.logOpenGLErrors);
        assertFalse(settings.logHexFontErrors);
        assertTrue(settings.alwaysTryNative);
        assertTrue(settings.debugPersistence);
        assertFalse(settings.nativeInTmpDir);
        assertTrue(settings.periodicallyForceLightUpdate);
        assertFalse(settings.insertIdsInConverters);
        assertTrue(settings.registerLuaJArchitecture);
        assertTrue(settings.disableLocaleChanging);
    }

    @Test
    public void parsesLegacyDebugCardAccessConfigurationValues() {
        Path configDirectory = Path.of("config-root");
        assertEquals(Settings.DebugCardAccess.Allowed.INSTANCE,
            DebugSettingsJava.parseDebugCardAccess(Boolean.TRUE, configDirectory));
        assertEquals(Settings.DebugCardAccess.Allowed.INSTANCE,
            DebugSettingsJava.parseDebugCardAccess("true", configDirectory));
        assertEquals(Settings.DebugCardAccess.Allowed.INSTANCE,
            DebugSettingsJava.parseDebugCardAccess("allow", configDirectory));
        assertEquals(Settings.DebugCardAccess.Forbidden.INSTANCE,
            DebugSettingsJava.parseDebugCardAccess(Boolean.FALSE, configDirectory));
        assertEquals(Settings.DebugCardAccess.Forbidden.INSTANCE,
            DebugSettingsJava.parseDebugCardAccess("false", configDirectory));
        assertEquals(Settings.DebugCardAccess.Forbidden.INSTANCE,
            DebugSettingsJava.parseDebugCardAccess("deny", configDirectory));

        Settings.DebugCardAccess.Whitelist whitelist = (Settings.DebugCardAccess.Whitelist)
            DebugSettingsJava.parseDebugCardAccess("whitelist", configDirectory);
        assertEquals(configDirectory.resolve("opencomputers/debug_card_whitelist.txt").toFile(), whitelist.noncesFile());
    }
}

