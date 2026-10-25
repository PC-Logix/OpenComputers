package li.cil.oc.common.nanomachines;

import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/** Java implementation of neural network NBT serialization. */
public final class NeuralNetworkPersistence {
    private static final String TRIGGERS_TAG = "triggers";
    private static final String IS_ACTIVE_TAG = "isActive";
    private static final String CONNECTORS_TAG = "connectors";
    private static final String BEHAVIORS_TAG = "behaviors";
    private static final String BEHAVIOR_TAG = "behavior";
    private static final String TRIGGER_INPUTS_TAG = "triggerInputs";
    private static final String CONNECTOR_INPUTS_TAG = "connectorInputs";

    private NeuralNetworkPersistence() { }

    public static void saveData(NeuralNetwork network, CompoundTag nbt, boolean forItem) {
        ListTag triggers = new ListTag();
        for (int i = 0; i < network.triggers.length(); i++) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean(IS_ACTIVE_TAG, network.triggers.apply(i).isActive() && !forItem);
            triggers.add(tag);
        }
        nbt.put(TRIGGERS_TAG, triggers);

        ListTag connectors = new ListTag();
        for (int i = 0; i < network.connectors.length(); i++) {
            NeuralNetwork.ConnectorNeuron connector = network.connectors.apply(i);
            CompoundTag tag = new CompoundTag();
            tag.putIntArray(TRIGGER_INPUTS_TAG, indices(connector, network, true));
            connectors.add(tag);
        }
        nbt.put(CONNECTORS_TAG, connectors);

        ListTag behaviors = new ListTag();
        for (int i = 0; i < network.behaviors.length(); i++) {
            NeuralNetwork.BehaviorNeuron behavior = network.behaviors.apply(i);
            CompoundTag tag = new CompoundTag();
            tag.putIntArray(TRIGGER_INPUTS_TAG, indices(behavior, network, true));
            tag.putIntArray(CONNECTOR_INPUTS_TAG, indices(behavior, network, false));
            tag.put(BEHAVIOR_TAG, behavior.provider.save(behavior.behavior));
            behaviors.add(tag);
        }
        nbt.put(BEHAVIORS_TAG, behaviors);
    }

    private static int[] indices(NeuralNetwork.ConnectorNeuron neuron, NeuralNetwork network, boolean triggers) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < neuron.inputs.length(); i++) {
            NeuralNetwork.Neuron input = neuron.inputs.apply(i);
            int index = triggers ? indexOfTrigger(network, input) : indexOfConnector(network, input);
            if (index >= 0) result.add(index);
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int indexOfTrigger(NeuralNetwork network, NeuralNetwork.Neuron input) {
        for (int i = 0; i < network.triggers.length(); i++) if (network.triggers.apply(i) == input) return i;
        return -1;
    }

    private static int indexOfConnector(NeuralNetwork network, NeuralNetwork.Neuron input) {
        for (int i = 0; i < network.connectors.length(); i++) if (network.connectors.apply(i) == input) return i;
        return -1;
    }

    public static void loadData(NeuralNetwork network, CompoundTag nbt, ControllerImpl controller) {
        network.triggers.clear();
        ListTag triggers = nbt.getList(TRIGGERS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < triggers.size(); i++) {
            CompoundTag tag = triggers.getCompound(i);
            NeuralNetwork.TriggerNeuron neuron = network.new TriggerNeuron();
            neuron.isActive_$eq(tag.getBoolean(IS_ACTIVE_TAG));
            network.triggers.append(neuron);
        }

        network.connectors.clear();
        ListTag connectors = nbt.getList(CONNECTORS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < connectors.size(); i++) {
            CompoundTag tag = connectors.getCompound(i);
            NeuralNetwork.ConnectorNeuron neuron = network.new ConnectorNeuron();
            for (int index : tag.getIntArray(TRIGGER_INPUTS_TAG)) neuron.inputs.append(network.triggers.apply(index));
            network.connectors.append(neuron);
        }

        network.behaviors.clear();
        ListTag behaviors = nbt.getList(BEHAVIORS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < behaviors.size(); i++) {
            CompoundTag tag = behaviors.getCompound(i);
            for (BehaviorProvider provider : Nanomachines.getProviders()) {
                Behavior behavior = provider.load(controller.player(), tag.getCompound(BEHAVIOR_TAG));
                if (behavior == null) continue;

                NeuralNetwork.BehaviorNeuron neuron = network.new BehaviorNeuron(provider, behavior);
                for (int index : tag.getIntArray(TRIGGER_INPUTS_TAG)) neuron.inputs.append(network.triggers.apply(index));
                for (int index : tag.getIntArray(CONNECTOR_INPUTS_TAG)) neuron.inputs.append(network.connectors.apply(index));
                network.behaviors.append(neuron);
                break;
            }
        }

        network.behaviorMap.clear();
        for (int i = 0; i < network.behaviors.length(); i++) {
            NeuralNetwork.BehaviorNeuron behavior = network.behaviors.apply(i);
            network.behaviorMap.put(behavior.behavior, behavior);
        }
    }
}
