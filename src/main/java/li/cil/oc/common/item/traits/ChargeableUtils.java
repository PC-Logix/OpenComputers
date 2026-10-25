package li.cil.oc.common.item.traits;

import li.cil.oc.Settings;

import java.util.function.DoubleConsumer;

public final class ChargeableUtils {
    private ChargeableUtils() {}

    public static double convertForgeEnergyToOpenComputers(int forgeEnergy) {
        return forgeEnergy / Settings.get().ratioForgeEnergy();
    }

    public static int convertOpenComputersToForgeEnergy(double openComputersEnergy) {
        return (int) (openComputersEnergy * Settings.get().ratioForgeEnergy());
    }

    public static double applyCharge(double amount, double current, double maximum, DoubleConsumer save) {
        double target = current + amount;
        double result = Math.min(Math.max(target, 0.0), maximum);
        double used = result - current;
        double unused = amount - used;
        if (used > Double.MIN_VALUE || used < -Double.MIN_VALUE) {
            save.accept(used);
        }
        return unused;
    }
}
