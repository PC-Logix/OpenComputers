package li.cil.oc.common.nanomachines;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.common.nanomachines.ControllerImpl;
import li.cil.oc.SettingsAccessJava;
import net.minecraft.server.level.ServerPlayer;

/** Java implementation of the controller's public nanomachine API methods. */
public final class ControllerApi {
    private ControllerApi() { }

    public static ControllerImpl reconfigure(ControllerImpl controller) {
        if (!controller.player().level().isClientSide()) {
            synchronized (controller.configuration()) {
                controller.configuration().reconfigure();
                controller.activeBehaviorsDirty_$eq(true);
                if (controller.player() instanceof ServerPlayer player && player.connection != null) {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 100));
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 150));
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 200));
                    controller.changeBuffer(-SettingsAccessJava.get().nanomachineReconfigureCost());
                    controller.hasSentConfiguration_$eq(false);
                }
            }
        }
        return controller;
    }

    public static int getTotalInputCount(ControllerImpl controller) {
        synchronized (controller.configuration()) { return controller.configuration().triggers.length(); }
    }

    public static int getSafeActiveInputs() { return SettingsAccessJava.get().nanomachinesSafeInputsActive(); }
    public static int getMaxActiveInputs() { return SettingsAccessJava.get().nanomachinesMaxInputsActive(); }

    public static boolean getInput(ControllerImpl controller, int index) {
        synchronized (controller.configuration()) { return controller.configuration().triggers.apply(index).isActive(); }
    }

    public static boolean setInput(ControllerImpl controller, int index, boolean value) {
        if (controller.player().level().isClientSide()) return false;
        synchronized (controller.configuration()) {
            int active = 0;
            for (int i = 0; i < controller.configuration().triggers.length(); i++) {
                if (controller.configuration().triggers.apply(i).isActive()) active++;
            }
            if (value && active >= SettingsAccessJava.get().nanomachinesMaxInputsActive()) return false;
            controller.configuration().triggers.apply(index).isActive_$eq(value);
            controller.activeBehaviorsDirty_$eq(true);
            return true;
        }
    }

    public static scala.collection.mutable.Set<Behavior> getActiveBehaviors(ControllerImpl controller) {
        synchronized (controller.configuration()) {
            ControllerBehaviorSync.clean(controller, DisableReason.InputChanged);
            return controller.activeBehaviors();
        }
    }

    public static int getInputCount(ControllerImpl controller, Behavior behavior) {
        synchronized (controller.configuration()) { return controller.configuration().inputs(behavior); }
    }

    public static double getLocalBuffer(ControllerImpl controller) { return controller.storedEnergy(); }
    public static double getLocalBufferSize() { return SettingsAccessJava.get().bufferNanomachines(); }

    public static double changeBuffer(ControllerImpl controller, double delta) {
        if (controller.player().level().isClientSide()) return delta;
        if (delta < 0 && (SettingsAccessJava.get().ignorePower() || controller.player().isCreative())) return 0.0;
        double newValue = controller.storedEnergy() + delta;
        controller.storedEnergy_$eq(Math.min(Math.max(newValue, 0), getLocalBufferSize()));
        return newValue - controller.storedEnergy();
    }
}
