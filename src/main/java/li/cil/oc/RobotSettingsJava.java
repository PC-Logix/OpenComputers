package li.cil.oc;

import com.typesafe.config.Config;

import java.util.List;

/** Robot behavior, experience progression, and action-delay configuration. */
public final class RobotSettingsJava {
    public final boolean allowActivateBlocks;
    public final boolean allowUseItemsWithDuration;
    public final boolean canAttackPlayers;
    public final int limitFlightHeight;
    public final boolean screwCobwebs;
    public final double swingRange;
    public final double useAndPlaceRange;
    public final double itemDamageRate;
    public final String nameFormat;
    public final String uuidFormat;
    public final int[] upgradeFlightHeight;
    public final double baseXpToLevel;
    public final double constantXpGrowth;
    public final double exponentialXpGrowth;
    public final double robotActionXp;
    public final double robotExhaustionXpRate;
    public final double robotOreXpRate;
    public final double bufferPerLevel;
    public final double toolEfficiencyPerLevel;
    public final double harvestSpeedBoostPerLevel;
    public final double turnDelay;
    public final double moveDelay;
    public final double swingDelay;
    public final double useDelay;
    public final double placeDelay;
    public final double dropDelay;
    public final double suckDelay;
    public final double harvestRatio;

    public RobotSettingsJava(Config config) {
        allowActivateBlocks = config.getBoolean("robot.allowActivateBlocks");
        allowUseItemsWithDuration = config.getBoolean("robot.allowUseItemsWithDuration");
        canAttackPlayers = config.getBoolean("robot.canAttackPlayers");
        limitFlightHeight = Math.max(-1, config.getInt("robot.limitFlightHeight"));
        screwCobwebs = config.getBoolean("robot.notAfraidOfSpiders");
        swingRange = config.getDouble("robot.swingRange");
        useAndPlaceRange = config.getDouble("robot.useAndPlaceRange");
        itemDamageRate = Math.min(atLeast(config.getDouble("robot.itemDamageRate"), 0), 1);
        nameFormat = config.getString("robot.nameFormat");
        uuidFormat = config.getString("robot.uuidFormat");
        upgradeFlightHeight = readIntArray(config, "robot.upgradeFlightHeight", 2,
            new int[]{64, 256}, "Bad number of hover flight height counts, ignoring.");

        baseXpToLevel = atLeast(config.getDouble("robot.xp.baseValue"), 0);
        constantXpGrowth = atLeast(config.getDouble("robot.xp.constantGrowth"), 1);
        exponentialXpGrowth = atLeast(config.getDouble("robot.xp.exponentialGrowth"), 1);
        robotActionXp = atLeast(config.getDouble("robot.xp.actionXp"), 0);
        robotExhaustionXpRate = atLeast(config.getDouble("robot.xp.exhaustionXpRate"), 0);
        robotOreXpRate = atLeast(config.getDouble("robot.xp.oreXpRate"), 0);
        bufferPerLevel = atLeast(config.getDouble("robot.xp.bufferPerLevel"), 0);
        toolEfficiencyPerLevel = atLeast(config.getDouble("robot.xp.toolEfficiencyPerLevel"), 0);
        harvestSpeedBoostPerLevel = atLeast(config.getDouble("robot.xp.harvestSpeedBoostPerLevel"), 0);

        turnDelay = atLeast(config.getDouble("robot.delays.turn") - 0.06, 0.05);
        moveDelay = atLeast(config.getDouble("robot.delays.move") - 0.06, 0.05);
        swingDelay = atLeast(config.getDouble("robot.delays.swing") - 0.06, 0);
        useDelay = atLeast(config.getDouble("robot.delays.use") - 0.06, 0);
        placeDelay = atLeast(config.getDouble("robot.delays.place") - 0.06, 0);
        dropDelay = atLeast(config.getDouble("robot.delays.drop") - 0.06, 0);
        suckDelay = atLeast(config.getDouble("robot.delays.suck") - 0.06, 0);
        harvestRatio = atLeast(config.getDouble("robot.delays.harvestRatio"), 0);
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

    private static double atLeast(double value, double minimum) {
        return value > minimum ? value : minimum;
    }
}
