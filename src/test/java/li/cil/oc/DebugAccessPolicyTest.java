package li.cil.oc;

import li.cil.oc.server.component.DebugCardAccessContext;
import org.junit.Test;
import java.nio.file.Files;
import java.util.Optional;
import static org.junit.Assert.*;

public final class DebugAccessPolicyTest {
    @Test
    public void allowAndDenyPoliciesExposeJavaAndLegacyChecksWithTheSameOutcome() {
        var allowed = Settings.DebugCardAccess.Allowed.INSTANCE;
        var forbidden = Settings.DebugCardAccess.Forbidden.INSTANCE;
        assertTrue(allowed.checkAccessJava(Optional.empty()).isEmpty());
        assertTrue(allowed.checkAccess(scala.Option.empty()).isEmpty());
        assertEquals("debug card is disabled", forbidden.checkAccessJava(Optional.empty()).orElseThrow());
        assertEquals("debug card is disabled", forbidden.checkAccess(scala.Option.empty()).get());
    }
    @Test
    public void configuredWhitelistPersistsAndRevokesNoncesThroughNativePolicy() throws Exception {
        var directory = Files.createTempDirectory("oc-debug-policy");
        var policy = (Settings.DebugCardAccess.Whitelist) DebugSettingsJava.parseDebugCardAccess("whitelist", directory);
        policy.add("Alice");
        String nonce = policy.nonceOptional("ALICE").orElseThrow();
        var context = new DebugCardAccessContext("Alice", nonce);
        var restored = (Settings.DebugCardAccess.Whitelist) DebugSettingsJava.parseDebugCardAccess("whitelist", directory);
        assertTrue(restored.checkAccessJava(Optional.of(context)).isEmpty());
        assertTrue(restored.checkAccess(scala.Option.apply(context)).isEmpty());
        assertEquals(java.util.Set.of("alice"), restored.whitelist());
        restored.invalidate("Alice");
        assertNotEquals(nonce, restored.nonceOptional("alice").orElseThrow());
        assertTrue(restored.checkAccessJava(Optional.of(context)).isPresent());
    }
}
