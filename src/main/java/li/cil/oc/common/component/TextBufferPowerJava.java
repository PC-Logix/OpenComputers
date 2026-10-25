package li.cil.oc.common.component;

import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.server.TextBufferPacketEncoder;
import li.cil.oc.util.PackedColor;
import net.minecraft.world.level.Level;

/** Power accounting helpers for text-buffer displays. */
final class TextBufferPowerJava {
    private TextBufferPowerJava() { }

    static double fullyLitCost(double costPerTick, int maximumWidth, int maximumHeight,
                               int tierOneWidth, int tierOneHeight) {
        return costPerTick * (maximumWidth * maximumHeight) / (tierOneWidth * tierOneHeight);
    }

    static double litArea(int[][] characters, int[][] colors, PackedColor.ColorFormat format,
                          int width, int height) {
        float litCells = 0;
        // Packet descriptions and old saves may resize the two planes separately.
        int safeHeight = Math.min(height, Math.min(characters.length, colors.length));
        for (int y = 0; y < safeHeight; y++) {
            int[] line = characters[y];
            int[] colorLine = colors[y];
            int safeWidth = Math.min(width, Math.min(line.length, colorLine.length));
            for (int x = 0; x < safeWidth; x++) {
                int character = line[x];
                int color = colorLine[x];
                int background = PackedColor.unpackBackground(color, format);
                int foreground = PackedColor.unpackForeground(color, format);
                if (character == ' ') {
                    if (background != 0) litCells++;
                } else if (character == 0x2588) {
                    if (foreground != 0) litCells++;
                } else if (foreground != 0 || background != 0) {
                    litCells++;
                }
            }
        }
        return width > 0 && height > 0 ? litCells / ((double) width * height) : 0;
    }

    static Boolean updatePower(TextBuffer screen, double tickFrequency) {
        if (!screen.isDisplayingForPowerAccounting()) return null;
        Level level = screen.host().getEnvironmentLevel();
        if (level.getGameTime() % tickFrequency != 0.0) return null;

        if (screen.relativeLitAreaForPowerAccounting() < 0) {
            screen.relativeLitAreaForPowerAccounting_$eq(litArea(
                screen.data().buffer,
                screen.data().color,
                screen.data().format(),
                screen.getViewportWidth(),
                screen.getViewportHeight()));
        }

        Node node = screen.node();
        if (node == null) return null;

        boolean hadPower = screen.hasPowerForPowerAccounting();
        double neededPower = screen.relativeLitAreaForPowerAccounting()
            * screen.fullyLitCost()
            * tickFrequency;
        boolean hasPower = ((Connector) node).tryChangeBuffer(-neededPower);
        screen.hasPowerForPowerAccounting_$eq(hasPower);
        return hasPower == hadPower ? null : hasPower;
    }

    static void updateAndSend(TextBuffer screen, double tickFrequency) throws java.io.IOException {
        Boolean powerChange = updatePower(screen, tickFrequency);
        if (powerChange == null) return;
        Boolean powerState = reportedPowerState(screen.isDisplayingForPowerAccounting(), powerChange);
        if (powerState != null) sendPowerChange(screen, powerState);
    }

    static void setEnergyCostPerTick(TextBuffer screen, double value) {
        screen.setPowerConsumptionPerTickForProxy(value);
        screen.setFullyLitCostForProxy(screen.computeFullyLitCost());
    }

    static Boolean setPowerState(TextBuffer screen, boolean value, double tickFrequency) {
        if (screen.isDisplayingForPowerAccounting() == value) return null;

        screen.setDisplayingForPowerAccounting(value);
        if (value) {
            double neededPower = screen.fullyLitCost() * tickFrequency;
            boolean hasPower = screen.node().changeBuffer(-neededPower) == 0;
            screen.hasPowerForPowerAccounting_$eq(hasPower);
        }
        return screen.isDisplayingForPowerAccounting() && screen.hasPowerForPowerAccounting();
    }

    static void setPowerStateAndSend(TextBuffer screen, boolean value, double tickFrequency)
            throws java.io.IOException {
        Boolean powerState = setPowerState(screen, value, tickFrequency);
        if (powerState != null) sendPowerChange(screen, powerState);
    }

    static Boolean reportedPowerState(boolean displaying, Boolean powerChange) {
        return powerChange == null ? null : displaying && powerChange;
    }

    private static void sendPowerChange(TextBuffer screen, boolean powerState) throws java.io.IOException {
        TextBufferPacketEncoder.sendPowerChange(screen.node().address(), powerState, screen.host());
    }
}
