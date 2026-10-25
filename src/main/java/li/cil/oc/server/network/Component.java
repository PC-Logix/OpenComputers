package li.cil.oc.server.network;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.server.driver.CompoundBlockEnvironment;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.server.machine.ArgumentsImpl;
import li.cil.oc.server.machine.CallbackAnalyzer;
import li.cil.oc.server.machine.Callbacks;
import li.cil.oc.server.machine.Machine;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedMutableDataComponentHolder;
import li.cil.oc.util.SideTracker;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Tuple2;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public interface Component extends li.cil.oc.api.network.Component, li.cil.oc.server.network.Node {
    // Node instances are assembled by the Java network builders.
    // These values do not refer back to the node, so weak keys can be collected.
    Map<Component, State> STATES = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    final class State {
        Visibility visibility = Visibility.None;
        Map<String, CallbackAnalyzer.Callback> callbacks;
    }

    private State componentState() {
        synchronized (STATES) { return STATES.computeIfAbsent(this, ignored -> new State()); }
    }

    private Map<String, CallbackAnalyzer.Callback> callbacks() {
        State state = componentState();
        synchronized (state) {
            if (state.callbacks == null) state.callbacks = Callbacks.apply(host());
            return state.callbacks;
        }
    }

    @Override default Visibility visibility() { return componentState().visibility; }

    @Override default void setVisibility(Visibility value) {
        if (value.ordinal() > reachability().ordinal())
            throw new IllegalArgumentException("Trying to set computer visibility to '" + value +
                "' on a '" + name() + "' node with reachability '" + reachability() +
                "'. It will be limited to the node's reachability.");
        if (!SideTracker.isServer()) return;
        Visibility previous = visibility();
        if (network() != null) {
            switch (previous) {
                case Neighbors -> {
                    if (value == Visibility.Network) addTo(reachableNodes());
                    else if (value == Visibility.None) removeFrom(neighbors());
                }
                case Network -> {
                    if (value == Visibility.Neighbors) {
                        Set<Node> neighborSet = new HashSet<>();
                        neighbors().forEach(neighborSet::add);
                        ArrayList<Node> distant = new ArrayList<>();
                        reachableNodes().forEach(node -> { if (!neighborSet.contains(node)) distant.add(node); });
                        removeFrom(distant);
                    } else if (value == Visibility.None) removeFrom(reachableNodes());
                }
                case None -> {
                    if (value == Visibility.Neighbors) addTo(neighbors());
                    else if (value == Visibility.Network) addTo(reachableNodes());
                }
            }
        }
        componentState().visibility = value;
    }

    @Override default boolean canBeSeenFrom(Node other) {
        return switch (visibility()) {
            case None -> false;
            case Network -> canBeReachedFrom(other);
            case Neighbors -> isNeighborOf(other);
        };
    }

    private void addTo(Iterable<Node> nodes) {
        for (Node node : nodes) if (node.host() instanceof Machine machine) machine.addComponent(this);
    }

    private void removeFrom(Iterable<Node> nodes) {
        for (Node node : nodes) if (node.host() instanceof Machine machine) machine.removeComponent(this);
    }

    @Override default Collection<String> methods() {
        return java.util.Collections.unmodifiableSet(callbacks().keySet());
    }

    @Override default Callback annotation(String method) {
        CallbackAnalyzer.Callback callback = callbacks().get(method);
        if (callback == null) throw Component.<RuntimeException>unchecked(new NoSuchMethodException());
        return callback.annotation;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException unchecked(Throwable error) throws E {
        throw (E) error;
    }

    @Override default Object[] invoke(String method, Context context, Object... arguments) throws Exception {
        CallbackAnalyzer.Callback callback = callbacks().get(method);
        if (callback == null) throw new NoSuchMethodException();
        Object target = host();
        if (target instanceof CompoundBlockEnvironment multi) {
            target = null;
            for (Tuple2<String, ManagedEnvironment> pair : CollectionConverters.asJava(multi.environments())) {
                ManagedEnvironment environment = pair._2();
                if (callback instanceof CallbackAnalyzer.ComponentCallback component &&
                    component.method.getDeclaringClass().isAssignableFrom(environment.getClass())) {
                    target = environment;
                    break;
                }
                if (callback instanceof CallbackAnalyzer.PeripheralCallback peripheral &&
                    environment instanceof ManagedPeripheral managed &&
                    Arrays.asList(managed.methods()).contains(peripheral.annotation.value())) {
                    target = environment;
                    break;
                }
            }
        }
        if (target == null) throw new NoSuchMethodException();
        return Registry.convert(callback.apply(target, context, new ArgumentsImpl(arguments)));
    }

    @Override default void loadData(DataComponentHolder holder) {
        li.cil.oc.server.network.Node.super.loadData(holder);
        ExtendedDataComponentHolder extended = holder instanceof MutableDataComponentHolder mutable
            ? new ExtendedMutableDataComponentHolder(mutable) : new ExtendedDataComponentHolder(holder);
        extended.getComponentOptional(OCComponentsJava.VISIBILITY).ifPresent(value -> componentState().visibility = value);
    }

    @Override default void saveData(MutableDataComponentHolder holder) {
        li.cil.oc.server.network.Node.super.saveData(holder);
        holder.set(OCComponentsJava.VISIBILITY, visibility());
    }
}
