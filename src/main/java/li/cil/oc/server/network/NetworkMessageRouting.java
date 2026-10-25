package li.cil.oc.server.network;

import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import scala.Option;
import scala.Option$;
import scala.collection.Iterable;
import scala.collection.Iterator;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.Map;

/** Selects the addressed nodes for each network message delivery mode. */
public final class NetworkMessageRouting {
    private NetworkMessageRouting() {}

    public static Option<Iterable<Node>> addressTargets(Map<String, NetworkVertex> data,
                                                        NetworkWrapper wrapper, Node source, String target) {
        validateSource(wrapper, source);
        Option<NetworkVertex> vertex = data.get(target);
        if (vertex.isDefined() && vertex.get().data.canBeReachedFrom(source)) {
            ArrayBuffer<Node> result = new ArrayBuffer<>();
            result.addOne(vertex.get().data);
            return Option$.MODULE$.apply(result);
        }
        return Option$.MODULE$.empty();
    }

    public static Iterable<Node> neighborTargets(Map<String, NetworkVertex> data,
                                                 NetworkWrapper wrapper, Node source) {
        validateSource(wrapper, source);
        ArrayBuffer<Node> result = new ArrayBuffer<>();
        Iterator<Node> neighbors = NetworkGraphQueries.neighbors(data, source).iterator();
        while (neighbors.hasNext()) {
            Node node = neighbors.next();
            if (node.reachability() != Visibility.None) result.addOne(node);
        }
        return result;
    }

    public static Iterable<Node> reachableTargets(Map<String, NetworkVertex> data,
                                                   NetworkWrapper wrapper, Node source) {
        validateSource(wrapper, source);
        return NetworkGraphQueries.reachableNodes(data, source);
    }

    public static Iterable<Node> visibleTargets(Map<String, NetworkVertex> data,
                                                 NetworkWrapper wrapper, Node source) {
        validateSource(wrapper, source);
        ArrayBuffer<Node> result = new ArrayBuffer<>();
        Iterator<Node> reachable = NetworkGraphQueries.reachableNodes(data, source).iterator();
        while (reachable.hasNext()) {
            Node node = reachable.next();
            if (node instanceof Component component && component.canBeSeenFrom(source)) result.addOne(node);
        }
        return result;
    }

    private static void validateSource(NetworkWrapper wrapper, Node source) {
        if (source.network() != wrapper) throw new IllegalArgumentException("Source node must be in this network.");
    }
}
