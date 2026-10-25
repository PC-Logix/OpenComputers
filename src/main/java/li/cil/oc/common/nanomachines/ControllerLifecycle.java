package li.cil.oc.common.nanomachines;

import li.cil.oc.api.Network;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.nbt.CompoundTag;

/** Java implementation of controller lifecycle and persistence operations. */
public final class ControllerLifecycle {
    private ControllerLifecycle() { }

    public static void reset(ControllerImpl controller) {
        synchronized (controller.configuration()) {
            NeuralNetwork configuration = controller.configuration();
            for (int i = 0; i < configuration.triggers.length(); i++) {
                configuration.triggers.apply(i).isActive_$eq(false);
                controller.activeBehaviorsDirty_$eq(true);
            }
            ControllerBehaviorSync.clean(controller, DisableReason.Default);
        }
    }

    public static void dispose(ControllerImpl controller) {
        reset(controller);
        if (!controller.player().level().isClientSide()) Network.leaveWirelessNetwork(controller);
    }

    public static void debug(ControllerImpl controller) {
        if (!controller.player().level().isClientSide()) {
            controller.configuration().debug();
            controller.activeBehaviorsDirty_$eq(true);
        }
    }

    public static void print(ControllerImpl controller) {
        if (!controller.player().level().isClientSide()) controller.configuration().print(controller.player());
    }

    public static void saveData(ControllerImpl controller, CompoundTag nbt) {
        synchronized (controller.configuration()) {
            nbt.putString("uuid", controller.uuid());
            nbt.putInt("port", controller.responsePort());
            nbt.putDouble("energy", controller.storedEnergy());
            CompoundTag configuration = new CompoundTag();
            controller.configuration().saveData(configuration, controller.player().level().registryAccess());
            nbt.put("configuration", configuration);
        }
    }

    public static void loadData(ControllerImpl controller, CompoundTag nbt) {
        synchronized (controller.configuration()) {
            controller.uuid_$eq(nbt.getString("uuid"));
            controller.responsePort_$eq(nbt.getInt("port"));
            controller.storedEnergy_$eq(nbt.getDouble("energy"));
            controller.configuration().loadData(nbt.getCompound("configuration"));
            controller.activeBehaviorsDirty_$eq(true);
        }
    }
}
