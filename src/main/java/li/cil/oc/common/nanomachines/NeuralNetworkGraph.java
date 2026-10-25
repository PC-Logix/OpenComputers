package li.cil.oc.common.nanomachines;

import li.cil.oc.OpenComputers$;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.server.GeneralPacketSender;
import li.cil.oc.SettingsAccessJava;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import scala.collection.mutable.ArrayBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/** Java implementation of nanomachine graph construction. */
public final class NeuralNetworkGraph {
    private NeuralNetworkGraph() { }

    public static void reconfigure(NeuralNetwork network, ControllerImpl controller) {
        network.behaviors.clear();
        for (BehaviorProvider provider : Nanomachines.getProviders()) {
            Iterable<Behavior> provided = provider.createBehaviors(controller.player());
            if (provided == null) continue;
            for (Behavior behavior : provided) {
                if (behavior != null) network.behaviors.append(network.new BehaviorNeuron(provider, behavior));
            }
        }

        int triggerCount = network.behaviors.length() * (int) SettingsAccessJava.get().nanomachineTriggerQuota();
        while (network.triggers.length() > triggerCount) network.triggers.remove(network.triggers.length() - 1);
        for (int i = 0; i < network.triggers.length(); i++) network.triggers.apply(i).isActive_$eq(false);
        while (network.triggers.length() < triggerCount) network.triggers.append(network.new TriggerNeuron());

        int connectorCount = network.behaviors.length() * (int) SettingsAccessJava.get().nanomachineConnectorQuota();
        while (network.connectors.length() > connectorCount) network.connectors.remove(network.connectors.length() - 1);
        for (int i = 0; i < network.connectors.length(); i++) network.connectors.apply(i).inputs.clear();
        while (network.connectors.length() < connectorCount) network.connectors.append(network.new ConnectorNeuron());

        Random random = new Random(controller.player().level().random.nextInt());
        List<NeuralNetwork.ConnectorNeuron> connectors = toList(network.connectors);
        List<NeuralNetwork.BehaviorNeuron> behaviors = toList(network.behaviors);
        List<NeuralNetwork.Neuron> sources = new ArrayList<>();
        for (int i = 0; i < SettingsAccessJava.get().nanomachineMaxOutputs(); i++) {
            for (int j = 0; j < network.triggers.length(); j++) sources.add(network.triggers.apply(j));
        }
        connect(connectors, sources, random);
        for (int i = 0; i < SettingsAccessJava.get().nanomachineMaxOutputs(); i++) {
            sources.addAll(connectors);
        }
        connect(behaviors, sources, random);

        List<NeuralNetwork.ConnectorNeuron> deadConnectors = new ArrayList<>();
        for (NeuralNetwork.ConnectorNeuron connector : connectors) {
            if (connector.inputs.isEmpty()) deadConnectors.add(connector);
        }
        for (NeuralNetwork.ConnectorNeuron dead : deadConnectors) network.connectors.$minus$eq(dead);
        for (NeuralNetwork.BehaviorNeuron behavior : behaviors) {
            for (NeuralNetwork.ConnectorNeuron dead : deadConnectors) behavior.inputs.$minus$eq(dead);
        }

        List<NeuralNetwork.BehaviorNeuron> deadBehaviors = new ArrayList<>();
        for (NeuralNetwork.BehaviorNeuron behavior : behaviors) {
            if (behavior.inputs.isEmpty()) deadBehaviors.add(behavior);
        }
        for (NeuralNetwork.BehaviorNeuron dead : deadBehaviors) network.behaviors.$minus$eq(dead);

        network.behaviorMap.clear();
        for (int i = 0; i < network.behaviors.length(); i++) {
            NeuralNetwork.BehaviorNeuron behavior = network.behaviors.apply(i);
            network.behaviorMap.put(behavior.behavior, behavior);
        }
    }

    private static <T> List<T> toList(ArrayBuffer<T> values) {
        List<T> result = new ArrayList<>(values.length());
        for (int i = 0; i < values.length(); i++) result.add(values.apply(i));
        return result;
    }

    private static <T extends NeuralNetwork.ConnectorNeuron> void connect(
            List<T> sinks, List<NeuralNetwork.Neuron> sources, Random random) {
        List<T> sinkPool = new ArrayList<>(sinks);
        Collections.shuffle(sinkPool, random);
        for (T sink : sinkPool) {
            if (sources.isEmpty()) break;
            HashSet<NeuralNetwork.Neuron> blacklist = new HashSet<>();
            int count = random.nextInt(SettingsAccessJava.get().nanomachineMaxInputs()) + 1;
            for (int n = 0; n < count && !sources.isEmpty(); n++) {
                int baseIndex = random.nextInt(sources.size());
                int sourceIndex = -1;
                for (int i = 0; i < sources.size(); i++) {
                    NeuralNetwork.Neuron candidate = sources.get((baseIndex + i) % sources.size());
                    if (!blacklist.contains(candidate)) { sourceIndex = i; break; }
                }
                if (sourceIndex >= 0) {
                    int selectedIndex = (sourceIndex + baseIndex) % sources.size();
                    NeuralNetwork.Neuron source = sources.remove(selectedIndex);
                    blacklist.add(source);
                    sink.inputs.append(source);
                }
            }
        }
    }

    public static void debug(NeuralNetwork network, ControllerImpl controller) {
        String message = "Creating debug configuration for nanomachines in player " +
                controller.player().getDisplayName().getString() + ".";
        if (controller.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            GeneralPacketSender.sendClientLogUnchecked(message, serverPlayer);
        } else {
            OpenComputers$.MODULE$.log().info(message);
        }

        network.behaviors.clear();
        for (BehaviorProvider provider : Nanomachines.getProviders()) {
            Iterable<Behavior> provided = provider.createBehaviors(controller.player());
            if (provided == null) continue;
            for (Behavior behavior : provided) {
                if (behavior != null) network.behaviors.append(network.new BehaviorNeuron(provider, behavior));
            }
        }
        network.connectors.clear();
        network.triggers.clear();
        for (int i = 0; i < network.behaviors.length(); i++) {
            NeuralNetwork.BehaviorNeuron behavior = network.behaviors.apply(i);
            NeuralNetwork.TriggerNeuron trigger = network.new TriggerNeuron();
            network.triggers.append(trigger);
            behavior.inputs.append(trigger);
            String name = i + " -> " + behavior.behavior.getNameHint() + " (" +
                    behavior.behavior.getClass() + ")";
            if (controller.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                GeneralPacketSender.sendClientLogUnchecked(name, serverPlayer);
            } else {
                OpenComputers$.MODULE$.log().info(name);
            }
        }
    }

    public static void print(NeuralNetwork network, Player player) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < network.behaviors.length(); i++) {
            NeuralNetwork.BehaviorNeuron behavior = network.behaviors.apply(i);
            String name = behavior.behavior.getNameHint();
            if (name == null) name = behavior.behavior.getClass().getSimpleName();
            appendColored(text, name, behavior.isActive());
            text.append(" <- (");
            boolean first = true;
            for (int j = 0; j < behavior.inputs.length(); j++) {
                if (!first) text.append(", ");
                first = false;
                NeuralNetwork.Neuron input = behavior.inputs.apply(j);
                if (input instanceof NeuralNetwork.TriggerNeuron trigger) {
                    appendColored(text, indexOfTrigger(network, trigger) + 1, trigger.isActive());
                } else if (input instanceof NeuralNetwork.ConnectorNeuron connector) {
                    text.append('(');
                    boolean firstTrigger = true;
                    for (int k = 0; k < connector.inputs.length(); k++) {
                        if (!firstTrigger) text.append(", ");
                        firstTrigger = false;
                        NeuralNetwork.Neuron triggerInput = connector.inputs.apply(k);
                        if (triggerInput instanceof NeuralNetwork.TriggerNeuron trigger) {
                            appendColored(text, indexOfTrigger(network, trigger) + 1, trigger.isActive());
                        }
                    }
                    text.append(')');
                }
            }
            text.append(')');
            player.sendSystemMessage(Component.literal(text.toString()));
            text.setLength(0);
        }
    }

    private static int indexOfTrigger(NeuralNetwork network, NeuralNetwork.TriggerNeuron trigger) {
        for (int i = 0; i < network.triggers.length(); i++) if (network.triggers.apply(i) == trigger) return i;
        return -1;
    }

    private static void appendColored(StringBuilder text, Object value, boolean enabled) {
        text.append(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)
                .append(value)
                .append(ChatFormatting.RESET);
    }
}
