package li.cil.oc;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Versioned settings migrations applied before defaults are merged. */
public final class SettingsConfigMigration {
    private static final String PREFIX = "opencomputers.";
    private static final VersionRange FILTERING_RULES_PATCH = versionRange("[0.0, 1.8.3)");
    private static final Pattern LEGACY_CIDR = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?:/(\\d{1,2}))");
    private static final ConfigPatch[] PATCHES = {
        new ConfigPatch("[0.0, 1.5.20)", new String[]{"switch.relayDelayUpgrade"}),
        new ConfigPatch("[0.0, 1.6.2)", new String[]{"nanomachines.potionWhitelist"}),
        new ConfigPatch("[0.0, 1.7.2)", new String[]{
            "power.cost.wirelessCostPerRange", "misc.maxWirelessRange", "misc.maxOpenPorts", "computer.cpuComponentCount"}),
        new ConfigPatch("[0.0, 1.8.0)", new String[]{"computer.robot.limitFlightHeight"}),
        new ConfigPatch("[0.0, 1.9.4)", new String[]{"misc.disassemblerBreakChance"})
    };

    private SettingsConfigMigration() {
    }

    private static VersionRange versionRange(String specification) {
        try {
            return VersionRange.createFromVersionSpec(specification);
        } catch (InvalidVersionSpecificationException error) {
            throw new IllegalArgumentException("Invalid built-in configuration version range: " + specification, error);
        }
    }

    public static Config patch(Config config, Config defaults) {
        String configVersionText = config.hasPath(PREFIX + "version")
            ? config.getString(PREFIX + "version") : "0.0.0";
        String currentVersionText = defaults.getString(PREFIX + "version");
        DefaultArtifactVersion configVersion = new DefaultArtifactVersion(configVersionText);
        DefaultArtifactVersion currentVersion = new DefaultArtifactVersion(currentVersionText);
        if (configVersion.compareTo(currentVersion) == 0) return config;

        OpenComputers.log().info("Updating config from version '" + configVersion + "' to '" + currentVersionText + "'.");
        Config patched = config.withValue(PREFIX + "version", defaults.getValue(PREFIX + "version"));
        for (ConfigPatch patch : PATCHES) {
            if (!patch.version().containsVersion(configVersion)) continue;
            for (String path : patch.paths()) {
                String fullPath = PREFIX + path;
                OpenComputers.log().info("=> Updating setting '" + fullPath + "'. ");
                if (defaults.hasPath(fullPath)) patched = patched.withValue(fullPath, defaults.getValue(fullPath));
                else patched = patched.withoutPath(fullPath);
            }
        }

        return migrateLegacyFilteringRules(patched, defaults, configVersion);
    }

    private static Config migrateLegacyFilteringRules(Config config, Config defaults, DefaultArtifactVersion configVersion) {
        String whitelistPath = PREFIX + "internet.whitelist";
        String blacklistPath = PREFIX + "internet.blacklist";
        if (!FILTERING_RULES_PATCH.containsVersion(configVersion) ||
            !config.hasPath(whitelistPath) || !config.hasPath(blacklistPath)) return config;

        OpenComputers.log().info("=> Migrating Internet Card filtering rules. ");
        List<String> whitelist = config.getStringList(whitelistPath);
        List<String> blacklist = config.getStringList(blacklistPath);
        List<String> rules = new ArrayList<>();
        for (String address : blacklist) rules.add(rule("deny", address));
        for (String address : whitelist) rules.add(rule("allow", address));
        if (!whitelist.isEmpty()) rules.add("deny all");
        rules.addAll(defaults.getStringList(PREFIX + "internet.filteringRules"));

        Config patched = config;
        try {
            for (String path : List.of(whitelistPath, blacklistPath)) {
                if (patched.hasPath(path)) {
                    ConfigValue originalValue = patched.getValue(path);
                    ConfigValue emptyValue = ConfigValueFactory.fromIterable(new ArrayList<String>(),
                        originalValue.origin().description());
                    patched = patched.withValue(path, emptyValue);
                }
            }
        } catch (Throwable ignored) {
            // Preserve the legacy behavior: failure to rewrite deprecated values does not cancel rule migration.
        }
        return patched.withValue(PREFIX + "internet.filteringRules", ConfigValueFactory.fromIterable(rules));
    }

    private static String rule(String action, String address) {
        return action + (LEGACY_CIDR.matcher(address).find() ? " ip:" : " domain:") + address;
    }

    private record ConfigPatch(VersionRange version, String[] paths) {
        private ConfigPatch(String version, String[] paths) {
            this(versionRange(version), paths);
        }
    }
}
