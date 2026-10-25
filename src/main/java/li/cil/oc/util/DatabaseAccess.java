package li.cil.oc.util;

import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.server.component.UpgradeDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class DatabaseAccess {
    private DatabaseAccess() {
    }

    public static Iterable<UpgradeDatabase> databases(Node node) {
        List<UpgradeDatabase> databases = new ArrayList<>();
        for (Node candidate : node.network().nodes()) {
            if (candidate instanceof Component component && component.host() instanceof UpgradeDatabase database) {
                databases.add(database);
            }
        }
        return databases;
    }

    public static UpgradeDatabase database(Node node, String address) {
        Node candidate = node.network().node(address);
        if (candidate instanceof Component component) {
            if (component.host() instanceof UpgradeDatabase database) {
                return database;
            }
            throw new IllegalArgumentException("not a database");
        }
        throw new IllegalArgumentException("no such component");
    }

    public static Object[] withDatabase(Node node, String address, Function<UpgradeDatabase, Object[]> action) {
        return action.apply(database(node, address));
    }
}
