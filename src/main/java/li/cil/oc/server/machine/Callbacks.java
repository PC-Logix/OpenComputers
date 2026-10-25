package li.cil.oc.server.machine;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.network.FilteredEnvironment;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.server.driver.CompoundBlockEnvironment;
import scala.Tuple2;
import scala.collection.Seq;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Discovers callbacks on component environments and caches class-only scans. */
public final class Callbacks {
    private static final CallbackAnalyzer ANALYZER = new CallbackAnalyzer(OpenComputers.log());
    private static final Map<Class<?>, Map<String, CallbackAnalyzer.Callback>> CACHE = new ConcurrentHashMap<>();

    public static Map<String, CallbackAnalyzer.Callback> apply(Object host) {
        if (host instanceof CompoundBlockEnvironment multi) {
            Seq<Tuple2<String, ManagedEnvironment>> environments = multi.environments();
            List<Object> values = new ArrayList<>();
            for (Tuple2<String, ManagedEnvironment> environment : CollectionConverters.asJava(environments)) {
                values.add(environment._2());
            }
            return ANALYZER.analyze(values);
        }
        if (host instanceof ManagedPeripheral || host instanceof FilteredEnvironment) {
            return ANALYZER.analyze(List.of(host));
        }
        return CACHE.computeIfAbsent(host.getClass(), ignored -> ANALYZER.analyze(List.of(host)));
    }

    public static void clear() {
        CACHE.clear();
    }

    public static Map<String, CallbackAnalyzer.Callback> fromClass(Class<?> environment) {
        return ANALYZER.fromClass(environment);
    }

    private Callbacks() {
    }
}
