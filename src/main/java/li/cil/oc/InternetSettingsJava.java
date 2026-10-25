package li.cil.oc;

import com.typesafe.config.Config;
import li.cil.oc.util.InternetFilteringRule;

import java.util.List;

/** HTTP, TCP, and request-filtering configuration. */
public final class InternetSettingsJava {
    public final boolean httpEnabled;
    public final boolean httpHeadersEnabled;
    public final boolean tcpEnabled;
    public final InternetFilteringRule[] filteringRules;
    public final boolean filteringRulesObserved;
    public final int httpTimeout;
    public final int maxConnections;
    public final int threads;
    public final String httpUserAgent;

    public InternetSettingsJava(Config config) {
        httpEnabled = config.getBoolean("internet.enableHttp");
        httpHeadersEnabled = config.getBoolean("internet.enableHttpHeaders");
        tcpEnabled = config.getBoolean("internet.enableTcp");

        List<String> configuredRules = config.getStringList("internet.filteringRules");
        filteringRules = configuredRules.stream()
            .filter(rule -> !rule.equals("removeme"))
            .map(InternetFilteringRule::new)
            .toArray(InternetFilteringRule[]::new);
        filteringRulesObserved = !configuredRules.contains("removeme");

        httpTimeout = Math.max(0, config.getInt("internet.requestTimeout")) * 1000;
        maxConnections = Math.max(0, config.getInt("internet.maxTcpConnections"));
        threads = Math.max(1, config.getInt("internet.threads"));
        httpUserAgent = config.getString("internet.httpUserAgent");
    }

    public boolean filteringRulesInvalid() {
        for (InternetFilteringRule rule : filteringRules) if (rule.invalid()) return true;
        return false;
    }

    public boolean internetAccessConfigured() {
        return httpEnabled || tcpEnabled;
    }

    public boolean internetAccessAllowed() {
        return internetAccessConfigured() && !filteringRulesInvalid();
    }
}
