package li.cil.oc.util;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import li.cil.oc.OpenComputers$;
import li.cil.oc.Settings;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class UpdateCheck {
    private static final String RELEASES_URL = "https://api.github.com/repos/CaitlynMainer/OpenComputers/releases";

    public static final CompletableFuture<Optional<UpdateRelease>> info = CompletableFuture.supplyAsync(UpdateCheck::initialize);

    private UpdateCheck() {}

    private static Optional<UpdateRelease> initialize() {
        // Keep the version template split up so it's not replaced with the actual version...
        if (Settings.get().updateCheck() && !OpenComputers$.MODULE$.Version().equals("@" + "VERSION" + "@")) {
            try {
                OpenComputers$.MODULE$.log().info("Starting OpenComputers version check.");
                JsonReader reader = new JsonReader(new InputStreamReader(URI.create(RELEASES_URL).toURL().openStream()));
                reader.beginArray();
                ArrayList<UpdateRelease> candidates = new ArrayList<>();
                while (reader.hasNext()) {
                    UpdateRelease release = new Gson().fromJson(reader, UpdateRelease.class);
                    if (!release.prerelease) {
                        boolean versionMatch = true;
                        if (release.tag_name.contains("/")) {
                            String[] tagNameParts = release.tag_name.split("/", 2);
                            if (tagNameParts.length >= 2) {
                                release.tag_name = tagNameParts[1];
                                versionMatch = Objects.equals(OpenComputers$.MODULE$.McVersion(), tagNameParts[0]);
                            }
                        }
                        if (versionMatch) candidates.add(release);
                    }
                }
                reader.endArray();
                if (!candidates.isEmpty()) {
                    UpdateRelease latest = candidates.stream().max((a, b) -> new ComparableVersion(a.tag_name.replaceFirst("^v", "")).compareTo(new ComparableVersion(b.tag_name.replaceFirst("^v", "")))).orElseThrow();
                    ComparableVersion remoteVersion = new ComparableVersion(latest.tag_name.replaceFirst("^v", ""));
                    ComparableVersion localVersion = new ComparableVersion(OpenComputers$.MODULE$.Version());
                    if (remoteVersion.compareTo(localVersion) > 0) {
                        OpenComputers$.MODULE$.log().info("A newer version of OpenComputers is available: " + latest.tag_name + ".");
                        return Optional.of(latest);
                    }
                }
                OpenComputers$.MODULE$.log().info("Running the latest OpenComputers version.");
            } catch (Throwable t) {
                OpenComputers$.MODULE$.log().warn("Update check for OpenComputers failed.", t);
            }
        }
        return Optional.empty();
    }
}
