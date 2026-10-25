package li.cil.oc.common.nanomachines;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.Persistable;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.HashMap;

/** Per-player nanomachine trigger and behavior graph. */
public class NeuralNetwork implements Persistable {
    private final ControllerImpl controller;
    public final ArrayBuffer<TriggerNeuron> triggers = new ArrayBuffer<>();
    public final ArrayBuffer<ConnectorNeuron> connectors = new ArrayBuffer<>();
    public final ArrayBuffer<BehaviorNeuron> behaviors = new ArrayBuffer<>();
    public final HashMap<Behavior, BehaviorNeuron> behaviorMap = new HashMap<>();

    public NeuralNetwork(ControllerImpl controller) {
        this.controller = controller;
    }

    public int inputs(Behavior behavior) {
        if (!behaviorMap.contains(behavior)) return 0;
        BehaviorNeuron node = behaviorMap.apply(behavior);
        int count = 0;
        for (int i = 0; i < node.inputs.length(); i++) if (node.inputs.apply(i).isActive()) count++;
        return count;
    }

    public void reconfigure() { NeuralNetworkGraph.reconfigure(this, controller); }
    public void debug() { NeuralNetworkGraph.debug(this, controller); }
    public void print(Player player) { NeuralNetworkGraph.print(this, player); }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        CompoundTag tag = new CompoundTag();
        NeuralNetworkPersistence.saveData(this, tag, false);
        holder.set(OCComponentsJava.NANOMACHINES_NETWORK_INFO.get(), tag);
    }

    @Override
    public void loadData(DataComponentHolder holder) {
        CompoundTag tag = holder.get(OCComponentsJava.NANOMACHINES_NETWORK_INFO.get());
        if (tag != null) loadData(tag);
    }

    public void saveData(CompoundTag tag, boolean forItem) {
        NeuralNetworkPersistence.saveData(this, tag, forItem);
    }

    public void loadData(CompoundTag tag) {
        NeuralNetworkPersistence.loadData(this, tag, controller);
    }

    public interface Neuron {
        boolean isActive();
    }

    public class TriggerNeuron implements Neuron {
        private boolean active;

        @Override
        public boolean isActive() { return active; }

        public void isActive_$eq(boolean value) { active = value; }
    }

    public class ConnectorNeuron implements Neuron {
        public final ArrayBuffer<Neuron> inputs = new ArrayBuffer<>();

        @Override
        public boolean isActive() {
            for (int i = 0; i < inputs.length(); i++) if (!inputs.apply(i).isActive()) return false;
            return true;
        }
    }

    public class BehaviorNeuron extends ConnectorNeuron {
        public final BehaviorProvider provider;
        public final Behavior behavior;

        public BehaviorNeuron(BehaviorProvider provider, Behavior behavior) {
            this.provider = provider;
            this.behavior = behavior;
        }
    }
}
