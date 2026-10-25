package li.cil.oc;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class InternetSettingsJavaTest {
    @Test
    public void filtersPlaceholderAndClampsNetworkLimits() {
        var config = ConfigFactory.parseString("""
            internet {
              enableHttp = true
              enableHttpHeaders = false
              enableTcp = true
              filteringRules = ["deny private", "removeme", "allow all"]
              requestTimeout = -2
              maxTcpConnections = -3
              threads = 0
              httpUserAgent = "OpenComputers/Test"
            }
            """);

        InternetSettingsJava settings = new InternetSettingsJava(config);

        assertTrue(settings.httpEnabled);
        assertFalse(settings.httpHeadersEnabled);
        assertTrue(settings.tcpEnabled);
        assertEquals(2, settings.filteringRules.length);
        assertEquals("deny private", settings.filteringRules[0].ruleString());
        assertEquals("allow all", settings.filteringRules[1].ruleString());
        assertFalse(settings.filteringRulesObserved);
        assertEquals(0, settings.httpTimeout);
        assertEquals(0, settings.maxConnections);
        assertEquals(1, settings.threads);
        assertEquals("OpenComputers/Test", settings.httpUserAgent);
        assertFalse(settings.filteringRulesInvalid());
        assertTrue(settings.internetAccessConfigured());
        assertTrue(settings.internetAccessAllowed());
    }

    @Test
    public void marksExplicitFilteringRulesAsObserved() {
        var config = ConfigFactory.parseString("""
            internet {
              enableHttp = false
              enableHttpHeaders = true
              enableTcp = false
              filteringRules = ["allow all"]
              requestTimeout = 3
              maxTcpConnections = 2
              threads = 4
              httpUserAgent = "OpenComputers/Test"
            }
            """);

        InternetSettingsJava settings = new InternetSettingsJava(config);

        assertTrue(settings.filteringRulesObserved);
        assertEquals(3000, settings.httpTimeout);
        assertEquals(2, settings.maxConnections);
        assertEquals(4, settings.threads);
        assertFalse(settings.internetAccessConfigured());
        assertFalse(settings.internetAccessAllowed());
    }
}
