package li.cil.oc;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class SwitchSettingsJavaTest {
    @Test
    public void enforcesMinimumQueueAndRelayLimits() {
        var config = ConfigFactory.parseString("""
            switch {
              defaultMaxQueueSize = 0
              queueSizeUpgrade = -1
              defaultRelayDelay = 0
              relayDelayUpgrade = -0.5
              defaultRelayAmount = -1
              relayAmountUpgrade = 2
            }
            """);

        SwitchSettingsJava settings = new SwitchSettingsJava(config);

        assertEquals(1, settings.defaultMaxQueueSize);
        assertEquals(0, settings.queueSizeUpgrade);
        assertEquals(1, settings.defaultRelayDelay);
        assertEquals(0.0, settings.relayDelayUpgrade, 0);
        assertEquals(1, settings.defaultRelayAmount);
        assertEquals(2, settings.relayAmountUpgrade);
    }
}
