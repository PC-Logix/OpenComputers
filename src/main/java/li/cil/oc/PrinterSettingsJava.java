package li.cil.oc;

import com.typesafe.config.Config;

/** Printer shape, material, and rendering configuration. */
public final class PrinterSettingsJava {
    public final int maxComplexity;
    public final double recycleRate;
    public final boolean chameliumEdible;
    public final int maxLightLevel;
    public final int customRedstoneCost;
    public final int materialValue;
    public final int inkValue;
    public final boolean printsHaveOpacity;
    public final double noclipMultiplier;

    public PrinterSettingsJava(Config config) {
        maxComplexity = config.getInt("printer.maxShapes");
        recycleRate = config.getDouble("printer.recycleRate");
        chameliumEdible = config.getBoolean("printer.chameliumEdible");
        maxLightLevel = Math.min(15, Math.max(0, config.getInt("printer.maxBaseLightLevel")));
        customRedstoneCost = Math.max(0, config.getInt("printer.customRedstoneCost"));
        materialValue = Math.max(0, config.getInt("printer.materialValue"));
        inkValue = Math.max(0, config.getInt("printer.inkValue"));
        printsHaveOpacity = config.getBoolean("printer.printsHaveOpacity");
        double configuredNoclipMultiplier = config.getDouble("printer.noclipMultiplier");
        noclipMultiplier = configuredNoclipMultiplier > 0 ? configuredNoclipMultiplier : 0;
    }
}
