package li.cil.oc;

import com.typesafe.config.Config;

import java.util.LinkedList;
import java.util.List;

/** Chunkloader dimension allow/deny lists. */
public final class ChunkloaderSettingsJava {
    public final List<Integer> dimensionBlacklist;
    public final List<Integer> dimensionWhitelist;

    public ChunkloaderSettingsJava(Config config) {
        dimensionBlacklist = intListOrEmpty(config, "chunkloader.dimBlacklist");
        dimensionWhitelist = intListOrEmpty(config, "chunkloader.dimWhitelist");
    }

    private static List<Integer> intListOrEmpty(Config config, String path) {
        return config.hasPath(path) ? config.getIntList(path) : new LinkedList<>();
    }
}
