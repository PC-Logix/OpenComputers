package li.cil.oc;

import com.typesafe.config.Config;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Debugging, Lua-runtime selection, and native-library options. */
public final class DebugSettingsJava {
    private final Config config;
    public final boolean logLuaCallbackErrors;
    public final boolean forceLuaJ;
    public final boolean allowUserdata;
    public final boolean allowPersistence;
    public final boolean limitMemory;
    public final boolean forceCaseInsensitive;
    public final boolean logFullLibLoadErrors;
    public final String forceNativeLibPlatform;
    public final String forceNativeLibPathFirst;
    public final boolean logOpenGLErrors;
    public final boolean logHexFontErrors;
    public final boolean alwaysTryNative;
    public final boolean debugPersistence;
    public final boolean nativeInTmpDir;
    public final boolean periodicallyForceLightUpdate;
    public final boolean insertIdsInConverters;
    public final boolean registerLuaJArchitecture;
    public final boolean disableLocaleChanging;

    public DebugSettingsJava(Config config) {
        this.config = config;
        logLuaCallbackErrors = config.getBoolean("debug.logCallbackErrors");
        forceLuaJ = config.getBoolean("debug.forceLuaJ");
        allowUserdata = !config.getBoolean("debug.disableUserdata");
        allowPersistence = !config.getBoolean("debug.disablePersistence");
        limitMemory = !config.getBoolean("debug.disableMemoryLimit");
        forceCaseInsensitive = config.getBoolean("debug.forceCaseInsensitiveFS");
        logFullLibLoadErrors = config.getBoolean("debug.logFullNativeLibLoadErrors");
        forceNativeLibPlatform = config.getString("debug.forceNativeLibPlatform");
        forceNativeLibPathFirst = config.getString("debug.forceNativeLibPathFirst");
        logOpenGLErrors = config.getBoolean("debug.logOpenGLErrors");
        logHexFontErrors = config.getBoolean("debug.logHexFontErrors");
        alwaysTryNative = config.getBoolean("debug.alwaysTryNative");
        debugPersistence = config.getBoolean("debug.verbosePersistenceErrors");
        nativeInTmpDir = config.getBoolean("debug.nativeInTmpDir");
        periodicallyForceLightUpdate = config.getBoolean("debug.periodicallyForceLightUpdate");
        insertIdsInConverters = config.getBoolean("debug.insertIdsInConverters");
        registerLuaJArchitecture = config.getBoolean("debug.registerLuaJArchitecture");
        disableLocaleChanging = config.getBoolean("debug.disableLocaleChanging");
    }

    public Settings.DebugCardAccess debugCardAccess() {
        return parseDebugCardAccess(config.getValue("debug.debugCardAccess").unwrapped(), FMLPaths.CONFIGDIR.get());
    }

    static Settings.DebugCardAccess parseDebugCardAccess(Object value, Path configDirectory) {
        if (Boolean.TRUE.equals(value) || "true".equals(value) || "allow".equals(value)) {
            return Settings.DebugCardAccess.Allowed.INSTANCE;
        }
        if (Boolean.FALSE.equals(value) || "false".equals(value) || "deny".equals(value)) {
            return Settings.DebugCardAccess.Forbidden.INSTANCE;
        }
        if ("whitelist".equals(value)) {
            Path whitelistPath = configDirectory.resolve(Paths.get("opencomputers", "debug_card_whitelist.txt"));
            return new Settings.DebugCardAccess.Whitelist(whitelistPath.toFile());
        }
        OpenComputers.log().warn("Unknown debug card access type, falling back to `deny`. Allowed values: `allow`, `deny`, `whitelist`.");
        return Settings.DebugCardAccess.Forbidden.INSTANCE;
    }
}

