package li.cil.oc.server.machine;

import scala.Tuple2;
import scala.collection.Iterable;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProgramLocations {
    private static final Map<String, Map<String, String>> ARCHITECTURE_LOCATIONS = new HashMap<>();
    private static final Map<String, String> GLOBAL_LOCATIONS = new HashMap<>();

    public static void addMapping(String program, String label, String... architectures) {
        if (architectures == null || architectures.length == 0) {
            GLOBAL_LOCATIONS.put(program, label);
        } else {
            for (String architecture : architectures) {
                ARCHITECTURE_LOCATIONS.computeIfAbsent(architecture, ignored -> new HashMap<>()).put(program, label);
            }
        }
    }

    public static Iterable<Tuple2<String, String>> getMappings(String architecture) {
        List<Tuple2<String, String>> mappings = new ArrayList<>();
        Map<String, String> local = ARCHITECTURE_LOCATIONS.get(architecture);
        if (local != null) {
            local.forEach((program, label) -> mappings.add(new Tuple2<>(program, label)));
        }
        GLOBAL_LOCATIONS.forEach((program, label) -> mappings.add(new Tuple2<>(program, label)));
        return CollectionConverters.asScala(mappings);
    }

    private ProgramLocations() {
    }
}
