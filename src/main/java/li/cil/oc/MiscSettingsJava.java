package li.cil.oc;

import com.typesafe.config.Config;
import li.cil.oc.common.Tier;

import java.util.List;

/** General runtime limits and gameplay options. */
public final class MiscSettingsJava {
    public final int maxScreenWidth;
    public final int maxScreenHeight;
    public final boolean inputUsername;
    public final int maxClipboardTextLength;
    public final int initialNetworkPacketTtl;
    public final int maxNetworkPacketSize;
    public final int maxNetworkPacketParts;
    public final double maxNetworkClientPacketDistance;
    public final double maxNetworkClientEffectPacketDistance;
    public final int[] maxOpenPorts;
    public final double[] maxWirelessRange;
    public final boolean updateCheck;
    public final int lootProbability;
    public final boolean lootRecrafting;
    public final int geolyzerRange;
    public final float geolyzerNoise;
    public final boolean disassembleAllTheThings;
    public final double disassemblerBreakChance;
    public final List<String> disassemblerInputBlacklist;
    public final boolean hideTier4;
    public final boolean hideOpenScreens;
    public final boolean hideOwnPet;
    public final boolean allowItemStackInspection;
    public final double presentChance;
    public final List<String> assemblerBlacklist;
    public final int threadPriority;
    public final boolean giveManualToNewPlayers;
    public final int dataCardSoftLimit;
    public final int dataCardHardLimit;
    public final double dataCardTimeout;
    public final int serverRackSwitchTier;
    public final double redstoneDelay;
    public final double tradingRange;
    public final int mfuRange;

    public MiscSettingsJava(Config config) {
        maxScreenWidth = Math.max(1, config.getInt("misc.maxScreenWidth"));
        maxScreenHeight = Math.max(1, config.getInt("misc.maxScreenHeight"));
        inputUsername = config.getBoolean("misc.inputUsername");
        maxClipboardTextLength = Math.max(0, config.getInt("misc.maxClipboard"));
        initialNetworkPacketTtl = Math.max(5, config.getInt("misc.initialNetworkPacketTTL"));
        maxNetworkPacketSize = Math.max(0, config.getInt("misc.maxNetworkPacketSize"));
        maxNetworkPacketParts = Math.max(4, config.getInt("misc.maxNetworkPacketParts"));
        maxNetworkClientPacketDistance = maxZero(config.getDouble("misc.maxNetworkClientPacketDistance"));
        maxNetworkClientEffectPacketDistance = maxZero(config.getDouble("misc.maxNetworkClientEffectPacketDistance"));
        maxOpenPorts = readIntArray(config, "misc.maxOpenPorts", 3, new int[]{16, 1, 16},
            "Bad number of max open ports, ignoring.");
        for (int i = 0; i < maxOpenPorts.length; i++) maxOpenPorts[i] = Math.max(0, maxOpenPorts[i]);
        maxWirelessRange = readDoubleArray(config, "misc.maxWirelessRange", 2, new double[]{16.0, 400.0},
            "Bad number of wireless card max ranges, ignoring.");
        for (int i = 0; i < maxWirelessRange.length; i++) maxWirelessRange[i] = maxZero(maxWirelessRange[i]);
        updateCheck = config.getBoolean("misc.updateCheck");
        lootProbability = config.getInt("misc.lootProbability");
        lootRecrafting = config.getBoolean("misc.lootRecrafting");
        geolyzerRange = config.getInt("misc.geolyzerRange");
        geolyzerNoise = maxZero((float) config.getDouble("misc.geolyzerNoise"));
        disassembleAllTheThings = config.getBoolean("misc.disassembleAllTheThings");
        disassemblerBreakChance = Math.min(maxZero(config.getDouble("misc.disassemblerBreakChance")), 1);
        disassemblerInputBlacklist = config.getStringList("misc.disassemblerInputBlacklist");
        hideTier4 = config.getBoolean("misc.hideTier4");
        hideOpenScreens = config.getBoolean("misc.hideOpenScreens");
        hideOwnPet = config.getBoolean("misc.hideOwnSpecial");
        allowItemStackInspection = config.getBoolean("misc.allowItemStackInspection");
        presentChance = Math.min(maxZero(config.getDouble("misc.presentChance")), 1);
        assemblerBlacklist = config.getStringList("misc.assemblerBlacklist");
        threadPriority = config.getInt("misc.threadPriority");
        giveManualToNewPlayers = config.getBoolean("misc.giveManualToNewPlayers");
        dataCardSoftLimit = Math.max(0, config.getInt("misc.dataCardSoftLimit"));
        dataCardHardLimit = Math.max(0, config.getInt("misc.dataCardHardLimit"));
        dataCardTimeout = maxZero(config.getDouble("misc.dataCardTimeout"));
        serverRackSwitchTier = Math.min(Tier.Four,
            Math.max(Tier.None, config.getInt("misc.serverRackSwitchTier") - 1));
        redstoneDelay = maxZero(config.getDouble("misc.redstoneDelay"));
        tradingRange = maxZero(config.getDouble("misc.tradingRange"));
        mfuRange = Math.min(128, Math.max(0, config.getInt("misc.mfuRange")));
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

    private static double maxZero(double value) {
        return value > 0 ? value : 0;
    }

    private static float maxZero(float value) {
        return value > 0 ? value : 0;
    }
}
