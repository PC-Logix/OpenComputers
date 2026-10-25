package li.cil.oc;

import li.cil.oc.api.internal.TextBuffer.ColorDepth;

import java.io.File;

/** Stable Java access to settings the loaded Java settings values. */
public final class SettingsAccessJava {
    private static volatile SettingsValuesJava current;

    private SettingsAccessJava() { }

    public static void install(SettingsValuesJava settings) {
        current = java.util.Objects.requireNonNull(settings, "settings");
    }

    public static void load(File file) {
        Settings.load(file);
    }

    public static SettingsValuesJava get() {
        SettingsValuesJava settings = current;
        if (settings == null) throw new IllegalStateException("Settings have not been loaded");
        return settings;
    }

    public static String namespace() {
        return Settings.namespace();
    }

    public static int screenWidthAtTier(int tier) {
        return ((Number) Settings.screenResolutionsByTier()[tier]._1()).intValue();
    }

    public static int screenHeightAtTier(int tier) {
        return ((Number) Settings.screenResolutionsByTier()[tier]._2()).intValue();
    }

    public static ColorDepth screenDepthAtTier(int tier) {
        return Settings.screenDepthsByTier()[tier];
    }
}

