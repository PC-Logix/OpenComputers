package li.cil.oc.server.driver;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.datacomponents.CompoundStorage;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Tuple2;
import scala.collection.immutable.Map;
import scala.collection.immutable.Map$;
import scala.collection.immutable.Seq;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CompoundBlockEnvironment implements ManagedEnvironment {
    private final String name;
    private final Seq<Tuple2<String, ManagedEnvironment>> environments;
    private final Component node;
    private final List<ManagedEnvironment> updatingEnvironments = new ArrayList<>();

    public CompoundBlockEnvironment(String name, Seq<Tuple2<String, ManagedEnvironment>> environments) {
        this.name = name;
        this.environments = environments;

        Visibility reachability = Visibility.None;
        var iterator = environments.iterator();
        while (iterator.hasNext()) {
            ManagedEnvironment environment = iterator.next()._2();
            Node wrappedNode = environment.node();
            if (wrappedNode != null && wrappedNode.reachability().ordinal() > reachability.ordinal()) {
                reachability = wrappedNode.reachability();
            }
        }
        node = Network.newNode(this, reachability).withComponent(name).create();
        iterator = environments.iterator();
        while (iterator.hasNext()) {
            ManagedEnvironment environment = iterator.next()._2();
            if (environment.canUpdate()) updatingEnvironments.add(environment);
            if (environment.node() instanceof Component component) component.setVisibility(Visibility.Neighbors);
        }
    }

    public String name() {
        return name;
    }

    public Seq<Tuple2<String, ManagedEnvironment>> environments() {
        return environments;
    }

    @Override
    public Component node() {
        return node;
    }

    @Override
    public boolean canUpdate() {
        var iterator = environments.iterator();
        while (iterator.hasNext()) {
            if (iterator.next()._2().canUpdate()) return true;
        }
        return false;
    }

    @Override
    public void update() {
        for (ManagedEnvironment environment : updatingEnvironments) environment.update();
    }

    @Override
    public void onMessage(Message message) {
    }

    @Override
    public void onConnect(Node connectedNode) {
        if (connectedNode != node) return;
        var iterator = environments.iterator();
        while (iterator.hasNext()) {
            Node wrappedNode = iterator.next()._2().node();
            if (wrappedNode != null) connectedNode.connect(wrappedNode);
        }
    }

    @Override
    public void onDisconnect(Node disconnectedNode) {
        if (disconnectedNode != node) return;
        var iterator = environments.iterator();
        while (iterator.hasNext()) {
            Node wrappedNode = iterator.next()._2().node();
            if (wrappedNode != null) wrappedNode.remove();
        }
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        Tuple2<Long, Map<String, CompoundStorage>> saved = holder.get(OCComponentsJava.COMPOUND_DRIVER);
        if (saved == null || !saved._1().equals(typeHash())) return;
        node.loadData(holder);

        Map<String, CompoundStorage> storage = saved._2();
        var iterator = environments.iterator();
        while (iterator.hasNext()) {
            Tuple2<String, ManagedEnvironment> entry = iterator.next();
            if (!storage.contains(entry._1())) continue;
            ManagedEnvironment environment = entry._2();
            try {
                environment.loadData(storage.apply(entry._1()));
            } catch (Throwable exception) {
                OpenComputers.log().warn("A block component of type '{}' (provided by driver '{}') threw an error while loading.",
                    environment.getClass().getName(), entry._1(), exception);
            }
        }
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        node.saveData(holder);
        Map<String, CompoundStorage> storage = Map$.MODULE$.empty();
        var iterator = environments.iterator();
        while (iterator.hasNext()) {
            Tuple2<String, ManagedEnvironment> entry = iterator.next();
            CompoundStorage saved = new CompoundStorage(DataComponentMap.EMPTY);
            entry._2().saveData(saved);
            storage = storage.updated(entry._1(), saved);
        }
        holder.set(OCComponentsJava.COMPOUND_DRIVER, new Tuple2<>(typeHash(), storage));
    }

    private long typeHash() {
        List<String> names = new ArrayList<>();
        var iterator = environments.iterator();
        while (iterator.hasNext()) names.add(iterator.next()._2().getClass().getName());
        Collections.sort(names);
        Hasher hash = Hashing.sha256().newHasher();
        for (String name : names) hash.putString(name, Charset.defaultCharset());
        return hash.hash().asLong();
    }
}
