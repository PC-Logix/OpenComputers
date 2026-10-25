import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import li.cil.oc.Settings;
import li.cil.oc.server.component.InternetCard;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InternetFilteringRuleTest {
    private Settings settings;

    @Before
    public void loadSettings() throws Exception {
        try (InputStream input = Settings.class.getResourceAsStream("/application.conf")) {
            if (input == null) throw new IllegalStateException("application.conf was not found");
            String configText;
            try (Scanner scanner = new Scanner(input, StandardCharsets.UTF_8)) {
                scanner.useDelimiter("\\A");
                configText = scanner.hasNext() ? scanner.next() : "";
            }
            Config config = ConfigFactory.parseString(configText);
            settings = new Settings(config.getConfig("opencomputers"));
        }
    }

    @Test public void acceptsValidExternalAddress() throws Exception { assertFalse(isUriBlacklisted("https://google.com")); }
    @Test public void rejectsLocalhost() throws Exception { assertTrue(isUriBlacklisted("http://localhost")); }

    @Test
    public void rejectsLoopbackIpv4Forms() throws Exception {
        assertTrue(isUriBlacklisted("http://127.0.0.1"));
        assertTrue(isUriBlacklisted("http://127.0.1"));
        assertTrue(isUriBlacklisted("http://127.1"));
        assertTrue(isUriBlacklisted("http://0"));
    }

    @Test
    public void rejectsLoopbackIpv6Forms() throws Exception {
        assertTrue(isUriBlacklisted("http://[::1]"));
        assertTrue(isUriBlacklisted("http://[::]"));
    }

    @Test
    public void rejectsIpv6Ipv4AddressEmbedding() throws Exception {
        assertTrue(isUriBlacklisted("http://[0:0:0:0:0:ffff:127.0.0.1]"));
        assertTrue(isUriBlacklisted("http://[::ffff:127.0.0.1]"));
    }

    @Test
    public void rejectsDecimalIpLocationBypasses() throws Exception {
        assertTrue(isUriBlacklisted("http://2130706433"));
        assertTrue(isUriBlacklisted("http://3232235521"));
        assertTrue(isUriBlacklisted("http://3232235777"));
    }

    @Test
    public void rejectsCloudMetadataAddresses() throws Exception {
        assertTrue(isUriBlacklisted("http://169.254.169.254"));
        assertTrue(isUriBlacklisted("http://2852039166"));
        assertTrue(isUriBlacklisted("http://[fd00:ec2::254]"));
        assertTrue(isUriBlacklisted("http://192.0.0.192"));
        assertTrue(isUriBlacklisted("http://100.100.100.200"));
    }

    private boolean isUriBlacklisted(String uri) throws Exception {
        URI uriObject = new URI(uri);
        InetAddress resolved = InetAddress.getByName(uriObject.getHost());
        return !InternetCard.isRequestAllowed(settings, resolved, uriObject.getHost());
    }
}
