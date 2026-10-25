package li.cil.oc;

import com.typesafe.config.Config;

import java.util.List;

/** GPU memory and rendering-cost configuration. */
public final class GpuSettingsJava {
    public final double[] vramSizes;
    public final double bitbltCost;
    public final int[] defaultResolution;

    public GpuSettingsJava(Config config) {
        vramSizes = readDoubleArray(config, "gpu.vramSizes", 4, new double[]{1, 2, 3, 4},
            "Bad number of VRAM sizes (expected 4), ignoring.");
        bitbltCost = config.hasPath("gpu.bitbltCost") ? config.getDouble("gpu.bitbltCost") : 0.5;
        defaultResolution = readResolution(config);
    }

    private static double[] readDoubleArray(Config config, String path, int expectedLength,
                                            double[] fallback, String warning) {
        List<Double> values = config.getDoubleList(path);
        if (values.size() != expectedLength) {
            OpenComputers.log().warn(warning);
            return fallback;
        }
        double[] result = new double[expectedLength];
        for (int i = 0; i < expectedLength; i++) result[i] = values.get(i);
        return result;
    }

    private static int[] readResolution(Config config) {
        List<Integer> values = config.getIntList("gpu.defaultResolution");
        if (values.size() == 2 && values.get(0) >= 0 && values.get(1) >= 0) {
            return new int[]{values.get(0), values.get(1)};
        }
        OpenComputers.log().warn("Bad default resolution (expected two non-negative integers), ignoring.");
        return new int[]{0, 0};
    }
}
