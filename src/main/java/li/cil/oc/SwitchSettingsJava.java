package li.cil.oc;

import com.typesafe.config.Config;

/** Wired switch queue and relay tuning. */
public final class SwitchSettingsJava {
    public final int defaultMaxQueueSize;
    public final int queueSizeUpgrade;
    public final int defaultRelayDelay;
    public final double relayDelayUpgrade;
    public final int defaultRelayAmount;
    public final int relayAmountUpgrade;

    public SwitchSettingsJava(Config config) {
        defaultMaxQueueSize = Math.max(1, config.getInt("switch.defaultMaxQueueSize"));
        queueSizeUpgrade = Math.max(0, config.getInt("switch.queueSizeUpgrade"));
        defaultRelayDelay = Math.max(1, config.getInt("switch.defaultRelayDelay"));
        double configuredDelayUpgrade = config.getDouble("switch.relayDelayUpgrade");
        relayDelayUpgrade = configuredDelayUpgrade > 0 ? configuredDelayUpgrade : 0;
        defaultRelayAmount = Math.max(1, config.getInt("switch.defaultRelayAmount"));
        relayAmountUpgrade = Math.max(0, config.getInt("switch.relayAmountUpgrade"));
    }
}
