package li.cil.oc;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public final class RobotSettingsJavaTest {
    @Test
    public void preservesRobotOptionsAndClampsProgressionAndDelays() {
        RobotSettingsJava settings = new RobotSettingsJava(config());

        assertEquals(-1, settings.limitFlightHeight);
        assertEquals(4.5, settings.swingRange, 0);
        assertEquals(6.5, settings.useAndPlaceRange, 0);
        assertEquals(1.0, settings.itemDamageRate, 0);
        assertArrayEquals(new int[]{80, 300}, settings.upgradeFlightHeight);
        assertEquals(0.0, settings.baseXpToLevel, 0);
        assertEquals(1.0, settings.constantXpGrowth, 0);
        assertEquals(1.5, settings.exponentialXpGrowth, 0);
        assertEquals(0.0, settings.robotActionXp, 0);
        assertEquals(0.0, settings.robotExhaustionXpRate, 0);
        assertEquals(2.0, settings.robotOreXpRate, 0);
        assertEquals(0.0, settings.bufferPerLevel, 0);
        assertEquals(0.5, settings.toolEfficiencyPerLevel, 0);
        assertEquals(0.0, settings.harvestSpeedBoostPerLevel, 0);
        assertEquals(0.05, settings.turnDelay, 0);
        assertEquals(0.94, settings.moveDelay, 1e-12);
        assertEquals(0.0, settings.swingDelay, 0);
        assertEquals(0.0, settings.useDelay, 0);
        assertEquals(0.01, settings.placeDelay, 1e-12);
        assertEquals(0.0, settings.dropDelay, 0);
        assertEquals(0.0, settings.suckDelay, 0);
        assertEquals(0.0, settings.harvestRatio, 0);
    }

    @Test
    public void usesDefaultFlightHeightsForInvalidArrayLength() {
        Config config = config().withValue("robot.upgradeFlightHeight", ConfigValueFactory.fromAnyRef(List.of(12)));
        RobotSettingsJava settings = new RobotSettingsJava(config);
        assertArrayEquals(new int[]{64, 256}, settings.upgradeFlightHeight);
    }

    private static Config config() {
        return ConfigFactory.parseString("""
            robot {
              allowActivateBlocks = true
              allowUseItemsWithDuration = false
              canAttackPlayers = true
              limitFlightHeight = -2
              notAfraidOfSpiders = true
              swingRange = 4.5
              useAndPlaceRange = 6.5
              itemDamageRate = 1.5
              nameFormat = "Robot %s"
              uuidFormat = "%s"
              upgradeFlightHeight = [80, 300]
              xp {
                baseValue = -1.0
                constantGrowth = -1.0
                exponentialGrowth = 1.5
                actionXp = -1.0
                exhaustionXpRate = -1.0
                oreXpRate = 2.0
                bufferPerLevel = -1.0
                toolEfficiencyPerLevel = 0.5
                harvestSpeedBoostPerLevel = -1.0
              }
              delays {
                turn = 0.02
                move = 1.0
                swing = 0.04
                use = 0.06
                place = 0.07
                drop = 0.0
                suck = 0.06
                harvestRatio = -1.0
              }
            }
            """);
    }
}
