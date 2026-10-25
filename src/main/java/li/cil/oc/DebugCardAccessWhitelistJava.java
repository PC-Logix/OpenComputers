package li.cil.oc;

import li.cil.oc.server.component.DebugCardAccessContext;
import org.apache.commons.codec.binary.Hex;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Nonce storage and access checks for the Java whitelist policy. */
public final class DebugCardAccessWhitelistJava {
    private final File noncesFile;
    private final Map<String, String> values = new HashMap<>();
    private final SecureRandom random;

    public DebugCardAccessWhitelistJava(File noncesFile) throws NoSuchAlgorithmException {
        this.noncesFile = noncesFile;
        this.random = SecureRandom.getInstance("SHA1PRNG");
    }

    public void save() throws IOException {
        File directory = noncesFile.getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Cannot create nonces directory: " + directory.getCanonicalPath());
        }

        try (var writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(noncesFile), StandardCharsets.UTF_8))) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                writer.write(entry.getKey());
                writer.write(' ');
                writer.write(entry.getValue());
                writer.newLine();
            }
        }
    }

    public void load() throws IOException {
        values.clear();
        if (!noncesFile.exists()) return;

        try (var reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(noncesFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 2);
                if (parts.length == 2) values.put(parts[0], parts[1]);
            }
        }
    }

    private String generateNonce() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return new String(Hex.encodeHex(bytes, true));
    }

    public Optional<String> nonce(String player) {
        return Optional.ofNullable(values.get(player.toLowerCase()));
    }

    public boolean isWhitelisted(String player) {
        return values.containsKey(player.toLowerCase());
    }

    public Set<String> whitelist() {
        return Collections.unmodifiableSet(values.keySet());
    }

    public void add(String player) throws IOException {
        String key = player.toLowerCase();
        if (!values.containsKey(key)) {
            values.put(key, generateNonce());
            save();
        }
    }

    public void remove(String player) throws IOException {
        if (values.remove(player.toLowerCase()) != null) save();
    }

    public void invalidate(String player) throws IOException {
        String key = player.toLowerCase();
        if (values.containsKey(key)) {
            values.put(key, generateNonce());
            save();
        }
    }

    public Optional<String> checkAccess(Optional<DebugCardAccessContext> context) {
        if (context.isEmpty()) {
            return Optional.of("debug card is whitelisted, Shift+Click with it to bind card to yourself");
        }
        DebugCardAccessContext access = context.get();
        String nonce = values.get(access.player().toLowerCase());
        if (nonce == null) return Optional.of("you are not whitelisted to use debug card");
        if (!nonce.equals(access.nonce())) {
            return Optional.of("debug card is invalidated, please re-bind it to yourself");
        }
        return Optional.empty();
    }
}

