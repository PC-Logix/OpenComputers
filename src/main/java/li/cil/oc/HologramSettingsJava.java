package li.cil.oc;

import com.typesafe.config.Config;

import java.util.List;

/** Hologram rendering bounds and behavior settings. */
public final class HologramSettingsJava {
    public final double[] maxScaleByTier;
    public final double[] maxTranslationByTier;
    public final double setRawDelay;
    public final boolean emitLight;

    public HologramSettingsJava(Config config) {
        maxScaleByTier = readArray(config, "hologram.maxScale",
            new double[]{3.0, 4.0, 5.0}, "Bad number of hologram max scales, ignoring.");
        maxTranslationByTier = readArray(config, "hologram.maxTranslation",
            new double[]{0.25, 0.5, 0.75}, "Bad number of hologram max translations, ignoring.");
        for (int i = 0; i < maxScaleByTier.length; i++) maxScaleByTier[i] = clampMinimum(maxScaleByTier[i], 1);
        for (int i = 0; i < maxTranslationByTier.length; i++) maxTranslationByTier[i] = clampMinimum(maxTranslationByTier[i], 0);
        setRawDelay = clampMinimum(config.getDouble("hologram.setRawDelay"), 0);
        emitLight = config.getBoolean("hologram.emitLight");
    }

    private static double[] readArray(Config config, String path, double[] fallback, String warning) {
        List<Double> values = config.getDoubleList(path);
        if (values.size() != 3) {
            OpenComputers.log().warn(warning);
            return fallback;
        }
        double[] result = new double[3];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i);
        return result;
    }

    private static double clampMinimum(double value, double minimum) {
        return value > minimum ? value : minimum;
    }
}
