package li.cil.oc;

import com.typesafe.config.Config;

import java.util.List;

/** Client display and visual-effect configuration; audio settings remain in Scala. */
public final class ClientVisualSettingsJava {
    public final double screenTextFadeStartDistance;
    public final double maxScreenTextRenderDistance;
    public final boolean textLinearFiltering;
    public final boolean textAntiAlias;
    public final boolean robotLabels;
    public final double fontCharScale;
    public final double hologramFadeStartDistance;
    public final double hologramRenderDistance;
    public final double hologramFlickerFrequency;
    public final int monochromeColor;
    public final String fontRenderer;
    public final double[] nanomachineHudPosition;
    public final boolean enableNanomachinePfx;
    public final int transposerFluidTransferRate;

    public ClientVisualSettingsJava(Config config) {
        screenTextFadeStartDistance = config.getDouble("client.screenTextFadeStartDistance");
        maxScreenTextRenderDistance = config.getDouble("client.maxScreenTextRenderDistance");
        textLinearFiltering = config.getBoolean("client.textLinearFiltering");
        textAntiAlias = config.getBoolean("client.textAntiAlias");
        robotLabels = config.getBoolean("client.robotLabels");
        fontCharScale = clamp(config.getDouble("client.fontCharScale"), 0.5, 2);
        hologramFadeStartDistance = atLeastZero(config.getDouble("client.hologramFadeStartDistance"));
        hologramRenderDistance = atLeastZero(config.getDouble("client.hologramRenderDistance"));
        hologramFlickerFrequency = atLeastZero(config.getDouble("client.hologramFlickerFrequency"));
        monochromeColor = Integer.decode(config.getString("client.monochromeColor"));
        fontRenderer = config.getString("client.fontRenderer");
        nanomachineHudPosition = readHudPosition(config);
        enableNanomachinePfx = config.getBoolean("client.enableNanomachinePfx");
        transposerFluidTransferRate = config.getInt("misc.transposerFluidTransferRate");
    }

    private static double[] readHudPosition(Config config) {
        List<Double> values = config.getDoubleList("client.nanomachineHudPos");
        if (values.size() == 2) return new double[]{values.get(0), values.get(1)};
        OpenComputers.log().warn("Bad number of HUD coordiantes, ignoring.");
        return new double[]{-1.0, -1.0};
    }

    private static double atLeastZero(double value) {
        return value > 0 ? value : 0;
    }

    private static double clamp(double value, double minimum, double maximum) {
        double lowerClamped = value > minimum ? value : minimum;
        return lowerClamped < maximum ? lowerClamped : maximum;
    }
}
