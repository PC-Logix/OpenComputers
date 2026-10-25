package li.cil.oc;

import com.typesafe.config.Config;

import java.util.List;

/** Computer and Lua runtime configuration parsing. */
public final class ComputerSettingsJava {
    private final int threads;
    private final double timeout;
    private final double startupDelay;
    private final int eepromSize;
    private final int eepromDataSize;
    private final int[] cpuComponentSupport;
    private final double[] callBudgets;
    private final boolean canComputersBeOwned;
    private final int maxUsers;
    private final int maxUsernameLength;
    private final boolean eraseTmpOnReboot;
    private final int executionDelay;
    private final boolean allowBytecode;
    private final boolean allowGC;
    private final boolean enableLua53;
    private final boolean defaultLua53;
    private final boolean enableLua54;
    private final int[] ramSizes;
    private final double ramScaleFor64Bit;
    private final int maxTotalRam;
    private final int maxSignalQueueSize;

    public ComputerSettingsJava(Config config) {
        threads = Math.max(1, config.getInt("computer.threads"));
        timeout = Math.max(0, config.getDouble("computer.timeout"));
        startupDelay = Math.max(0.05, config.getDouble("computer.startupDelay"));
        eepromSize = Math.max(0, config.getInt("computer.eepromSize"));
        eepromDataSize = Math.max(0, config.getInt("computer.eepromDataSize"));
        cpuComponentSupport = readIntArray(config, "computer.cpuComponentCount", 5,
            new int[]{8, 12, 16, 20, 1024}, "Bad number of CPU component counts, ignoring.");
        callBudgets = readDoubleArray(config, "computer.callBudgets", 4,
            new double[]{0.5, 1.0, 1.5, 2.0}, "Bad number of call budgets, ignoring.");
        canComputersBeOwned = config.getBoolean("computer.canComputersBeOwned");
        maxUsers = Math.max(0, config.getInt("computer.maxUsers"));
        maxUsernameLength = Math.max(0, config.getInt("computer.maxUsernameLength"));
        eraseTmpOnReboot = config.getBoolean("computer.eraseTmpOnReboot");
        executionDelay = Math.max(0, config.getInt("computer.executionDelay"));

        allowBytecode = config.getBoolean("computer.lua.allowBytecode");
        allowGC = config.getBoolean("computer.lua.allowGC");
        enableLua53 = config.getBoolean("computer.lua.enableLua53");
        defaultLua53 = config.getBoolean("computer.lua.defaultLua53");
        enableLua54 = config.getBoolean("computer.lua.enableLua54");
        ramSizes = readIntArray(config, "computer.lua.ramSizes", 8,
            new int[]{256, 384, 512, 768, 1024, 1536, 2048, 3072}, "Bad number of RAM sizes, ignoring.");
        ramScaleFor64Bit = Math.max(1, config.getDouble("computer.lua.ramScaleFor64Bit"));
        maxTotalRam = Math.max(0, config.getInt("computer.lua.maxTotalRam"));
        maxSignalQueueSize = Math.max(256,
            config.hasPath("computer.maxSignalQueueSize") ? config.getInt("computer.maxSignalQueueSize") : 256);
    }

    private static int[] readIntArray(Config config, String path, int expectedLength,
                                      int[] fallback, String warning) {
        List<Integer> values = config.getIntList(path);
        if (values.size() != expectedLength) {
            OpenComputers.log().warn(warning);
            return fallback;
        }
        int[] result = new int[expectedLength];
        for (int i = 0; i < expectedLength; i++) result[i] = values.get(i);
        return result;
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

    public int threads() { return threads; }
    public double timeout() { return timeout; }
    public double startupDelay() { return startupDelay; }
    public int eepromSize() { return eepromSize; }
    public int eepromDataSize() { return eepromDataSize; }
    public int[] cpuComponentSupport() { return cpuComponentSupport; }
    public double[] callBudgets() { return callBudgets; }
    public boolean canComputersBeOwned() { return canComputersBeOwned; }
    public int maxUsers() { return maxUsers; }
    public int maxUsernameLength() { return maxUsernameLength; }
    public boolean eraseTmpOnReboot() { return eraseTmpOnReboot; }
    public int executionDelay() { return executionDelay; }
    public boolean allowBytecode() { return allowBytecode; }
    public boolean allowGC() { return allowGC; }
    public boolean enableLua53() { return enableLua53; }
    public boolean defaultLua53() { return defaultLua53; }
    public boolean enableLua54() { return enableLua54; }
    public int[] ramSizes() { return ramSizes; }
    public double ramScaleFor64Bit() { return ramScaleFor64Bit; }
    public int maxTotalRam() { return maxTotalRam; }
    public int maxSignalQueueSize() { return maxSignalQueueSize; }
}
