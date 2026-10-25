package li.cil.oc;

import com.typesafe.config.Config;

import java.util.List;

/** Validation for legacy options that were removed from the user configuration. */
public final class SettingsConfigValidation {
    private static final String PREFIX = "opencomputers.";
    private static final List<String> REMOVED_LIST_OPTIONS = List.of(
        "internet.blacklist",
        "internet.whitelist"
    );

    private SettingsConfigValidation() { }

    public static void validateRemovedListOptions(Config config) {
        for (String key : REMOVED_LIST_OPTIONS) {
            String path = PREFIX + key;
            if (config.hasPath(path) && !config.getStringList(path).isEmpty()) {
                throw new RuntimeException("Error parsing configuration file: removed configuration option '" + key
                    + "' is not empty. This option should no longer be used.");
            }
        }
    }
}
