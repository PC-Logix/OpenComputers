package li.cil.oc;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ComputerSettingsJavaTest {
    @Test
    public void clampsValuesAndPreservesConfiguredTierArrays() {
        var config = ConfigFactory.parseString("""
            computer {
              threads = 0
              timeout = -1
              startupDelay = 0
              eepromSize = -1
              eepromDataSize = 0
              cpuComponentCount = [1, 2, 3, 4, 5]
              callBudgets = [0.1, 0.2, 0.3, 0.4]
              canComputersBeOwned = true
              maxUsers = -2
              maxUsernameLength = -3
              eraseTmpOnReboot = true
              executionDelay = -4
              maxSignalQueueSize = 128
              lua {
                allowBytecode = true
                allowGC = false
                enableLua53 = true
                defaultLua53 = false
                enableLua54 = true
                ramSizes = [256, 384, 512, 768, 1024, 1536, 2048, 3072]
                ramScaleFor64Bit = 0
                maxTotalRam = -1
              }
            }
            """);

        ComputerSettingsJava settings = new ComputerSettingsJava(config);

        assertEquals(1, settings.threads());
        assertEquals(0.0, settings.timeout(), 0.0);
        assertEquals(0.05, settings.startupDelay(), 0.0);
        assertEquals(0, settings.eepromSize());
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, settings.cpuComponentSupport());
        assertArrayEquals(new double[]{0.1, 0.2, 0.3, 0.4}, settings.callBudgets(), 0.0);
        assertTrue(settings.canComputersBeOwned());
        assertEquals(0, settings.maxUsers());
        assertEquals(0, settings.maxUsernameLength());
        assertEquals(0, settings.executionDelay());
        assertEquals(256, settings.maxSignalQueueSize());
        assertArrayEquals(new int[]{256, 384, 512, 768, 1024, 1536, 2048, 3072}, settings.ramSizes());
        assertEquals(1.0, settings.ramScaleFor64Bit(), 0.0);
        assertEquals(0, settings.maxTotalRam());
    }
}
