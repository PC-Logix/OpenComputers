package li.cil.oc.integration.util;

import li.cil.oc.Settings;

public final class Power {
    private Power() {}

    public static double fromAE(double value) {
        return value * Settings.get().ratioAppliedEnergistics2();
    }

    public static double toAE(double value) {
        return value / Settings.get().ratioAppliedEnergistics2();
    }

    public static double fromJoules(double value) {
        return value * Settings.get().ratioMekanism();
    }

    public static double toJoules(double value) {
        return value / Settings.get().ratioMekanism();
    }

    public static double fromRF(int value) {
        return value * Settings.get().ratioRedstoneFlux();
    }

    public static int toRF(double value) {
        return (int) (value / Settings.get().ratioRedstoneFlux());
    }
}
