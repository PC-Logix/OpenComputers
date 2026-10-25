package li.cil.oc;

import com.typesafe.config.Config;

import java.util.List;

/** Nanomachine control, range, and resource limits. */
public final class NanomachineSettingsJava {
    public final double triggerQuota;
    public final double connectorQuota;
    public final int maxInputs;
    public final int maxOutputs;
    public final int safeInputsActive;
    public final int maxInputsActive;
    public final double commandDelay;
    public final double commandRange;
    public final double magnetRange;
    public final int disintegrationRange;
    public final List<?> potionWhitelist;
    public final float hungryDamage;
    public final double hungryEnergyRestored;

    public NanomachineSettingsJava(Config config) {
        triggerQuota = nonNegative(config.getDouble("nanomachines.triggerQuota"));
        connectorQuota = nonNegative(config.getDouble("nanomachines.connectorQuota"));
        maxInputs = Math.max(1, config.getInt("nanomachines.maxInputs"));
        maxOutputs = Math.max(1, config.getInt("nanomachines.maxOutputs"));
        safeInputsActive = Math.max(0, config.getInt("nanomachines.safeInputsActive"));
        maxInputsActive = Math.max(0, config.getInt("nanomachines.maxInputsActive"));
        commandDelay = nonNegative(config.getDouble("nanomachines.commandDelay"));
        commandRange = nonNegative(config.getDouble("nanomachines.commandRange"));
        magnetRange = nonNegative(config.getDouble("nanomachines.magnetRange"));
        disintegrationRange = Math.max(0, config.getInt("nanomachines.disintegrationRange"));
        potionWhitelist = config.getAnyRefList("nanomachines.potionWhitelist");
        hungryDamage = nonNegative((float) config.getDouble("nanomachines.hungryDamage"));
        hungryEnergyRestored = nonNegative(config.getDouble("nanomachines.hungryEnergyRestored"));
    }

    private static double nonNegative(double value) {
        return value > 0 ? value : 0;
    }

    private static float nonNegative(float value) {
        return value > 0 ? value : 0;
    }
}
