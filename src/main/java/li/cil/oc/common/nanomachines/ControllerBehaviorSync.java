package li.cil.oc.common.nanomachines;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.server.NanomachinePacketSender;
import scala.collection.mutable.HashSet;

/** Java implementation of active nanomachine behavior synchronization. */
public final class ControllerBehaviorSync {
    private ControllerBehaviorSync() { }

    public static void clean(ControllerImpl controller, DisableReason reason) {
        if (!controller.activeBehaviorsDirty()) return;

        HashSet<Behavior> newBehaviors = new HashSet<>();
        NeuralNetwork network = controller.configuration();
        for (int i = 0; i < network.behaviors.length(); i++) {
            NeuralNetwork.BehaviorNeuron neuron = network.behaviors.apply(i);
            if (neuron.isActive()) newBehaviors.add(neuron.behavior);
        }

        HashSet<Behavior> addedBehaviors = new HashSet<>();
        HashSet<Behavior> removedBehaviors = new HashSet<>();
        scala.collection.Iterator<Behavior> iterator = newBehaviors.iterator();
        while (iterator.hasNext()) {
            Behavior behavior = iterator.next();
            if (!controller.activeBehaviors().contains(behavior)) addedBehaviors.add(behavior);
        }
        iterator = controller.activeBehaviors().iterator();
        while (iterator.hasNext()) {
            Behavior behavior = iterator.next();
            if (!newBehaviors.contains(behavior)) removedBehaviors.add(behavior);
        }

        controller.activeBehaviors().clear();
        iterator = newBehaviors.iterator();
        while (iterator.hasNext()) controller.activeBehaviors().add(iterator.next());
        controller.activeBehaviorsDirty_$eq(false);

        iterator = addedBehaviors.iterator();
        while (iterator.hasNext()) iterator.next().onEnable();
        iterator = removedBehaviors.iterator();
        while (iterator.hasNext()) iterator.next().onDisable(reason);

        if (!controller.player().level().isClientSide()) {
            NanomachinePacketSender.sendInputsUnchecked(controller.player());
        }
    }
}
