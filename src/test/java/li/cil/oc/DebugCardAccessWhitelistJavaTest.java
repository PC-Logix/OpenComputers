package li.cil.oc;

import li.cil.oc.server.component.DebugCardAccessContext;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public final class DebugCardAccessWhitelistJavaTest {
    @Test
    public void persistsNoncesAndChecksAccessAcrossReloads() throws Exception {
        Path directory = Files.createTempDirectory("oc-debug-whitelist");
        var whitelist = new DebugCardAccessWhitelistJava(directory.resolve("nonces.txt").toFile());
        whitelist.load();
        assertFalse(whitelist.isWhitelisted("Alice"));

        whitelist.add("Alice");
        String nonce = whitelist.nonce("ALICE").get();
        assertTrue(nonce.matches("[0-9a-f]{32}"));
        assertEquals("debug card is whitelisted, Shift+Click with it to bind card to yourself",
            whitelist.checkAccess(Optional.empty()).get());

        var reloaded = new DebugCardAccessWhitelistJava(directory.resolve("nonces.txt").toFile());
        reloaded.load();
        assertTrue(reloaded.isWhitelisted("alice"));
        assertEquals(nonce, reloaded.nonce("alice").get());
        assertEquals(Optional.empty(), reloaded.checkAccess(Optional.of(new DebugCardAccessContext("Alice", nonce))));
        assertEquals("debug card is invalidated, please re-bind it to yourself",
            reloaded.checkAccess(Optional.of(new DebugCardAccessContext("Alice", "wrong"))).get());

        reloaded.invalidate("alice");
        String replacement = reloaded.nonce("alice").get();
        assertNotEquals(nonce, replacement);
        reloaded.remove("ALICE");
        assertFalse(reloaded.isWhitelisted("alice"));
        assertTrue(Files.exists(directory.resolve("nonces.txt")));
    }
}
