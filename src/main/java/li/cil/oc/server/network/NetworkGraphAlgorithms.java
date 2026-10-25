package li.cil.oc.server.network;

import scala.collection.Iterator;
import scala.collection.Seq;
import scala.Tuple2;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.HashMap;
import scala.collection.mutable.Map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Connectivity traversal used to split a network after an edge or node is removed. */
public final class NetworkGraphAlgorithms {
    private NetworkGraphAlgorithms() {}

    public static Seq<Map<String, NetworkVertex>> searchGraphs(Seq<NetworkVertex> seeds) {
        Set<NetworkVertex> seen = new HashSet<>();
        ArrayBuffer<Map<String, NetworkVertex>> graphs = new ArrayBuffer<>();
        Iterator<NetworkVertex> seedIterator = seeds.iterator();
        while (seedIterator.hasNext()) {
            NetworkVertex seed = seedIterator.next();
            if (seen.contains(seed)) continue;

            Map<String, NetworkVertex> addressed = new HashMap<>();
            ArrayDeque<NetworkVertex> queue = new ArrayDeque<>();
            queue.add(seed);
            while (!queue.isEmpty()) {
                NetworkVertex vertex = queue.remove();
                seen.add(vertex);
                addressed.update(vertex.data.address(), vertex);

                Iterator<NetworkEdge> edges = vertex.edges.iterator();
                while (edges.hasNext()) {
                    NetworkVertex neighbor = edges.next().other(vertex);
                    if (!seen.contains(neighbor) && !queue.contains(neighbor)) queue.add(neighbor);
                }
            }
            graphs.addOne(addressed);
        }
        return graphs;
    }

    public static void handleSplit(NetworkCore network, Map<String, NetworkVertex> data,
                                   ArrayBuffer<Connector> connectors,
                                   Seq<Map<String, NetworkVertex>> subGraphs,
                                   Consumer<Map<String, NetworkVertex>> createNetwork) {
        if (subGraphs.size() <= 1) return;

        List<List<Node>> nodes = new ArrayList<>(subGraphs.size());
        List<List<Node>> visibleNodes = new ArrayList<>(subGraphs.size());
        Iterator<Map<String, NetworkVertex>> graphIterator = subGraphs.iterator();
        while (graphIterator.hasNext()) {
            List<Node> graphNodes = new ArrayList<>();
            List<Node> visibleGraphNodes = new ArrayList<>();
            Iterator<NetworkVertex> vertices = graphIterator.next().values().iterator();
            while (vertices.hasNext()) {
                Node node = vertices.next().data;
                graphNodes.add(node);
                if (node.reachability() == li.cil.oc.api.network.Visibility.Network) visibleGraphNodes.add(node);
            }
            nodes.add(graphNodes);
            visibleNodes.add(visibleGraphNodes);
        }

        data.clear();
        connectors.clear();
        network.globalBuffer_$eq(0);
        network.globalBufferSize_$eq(0);
        Iterator<Tuple2<String, NetworkVertex>> entries = subGraphs.apply(0).iterator();
        while (entries.hasNext()) {
            Tuple2<String, NetworkVertex> entry = entries.next();
            data.update(entry._1(), entry._2());
        }
        Iterator<NetworkVertex> vertices = data.values().iterator();
        while (vertices.hasNext()) {
            li.cil.oc.api.network.Node node = vertices.next().data;
            if (node instanceof Connector connector) network.addConnector(connector);
        }

        Iterator<Map<String, NetworkVertex>> remainingGraphs = subGraphs.iterator();
        if (remainingGraphs.hasNext()) remainingGraphs.next();
        while (remainingGraphs.hasNext()) createNetwork.accept(remainingGraphs.next());

        for (int indexA = 0; indexA < subGraphs.size(); indexA++) {
            List<Node> nodesA = nodes.get(indexA);
            List<Node> visibleNodesA = visibleNodes.get(indexA);
            for (int indexB = indexA + 1; indexB < subGraphs.size(); indexB++) {
                List<Node> nodesB = nodes.get(indexB);
                List<Node> visibleNodesB = visibleNodes.get(indexB);
                for (Node node : visibleNodesA) {
                    for (Node disconnected : nodesB) disconnected.onDisconnect(node);
                }
                for (Node node : visibleNodesB) {
                    for (Node disconnected : nodesA) disconnected.onDisconnect(node);
                }
            }
        }
    }
}
