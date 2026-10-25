package li.cil.oc.common.nanomachines;

import li.cil.oc.api.Network;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.server.NanomachinePacketSender;
import li.cil.oc.util.PlayerUtils;
import li.cil.oc.SettingsAccessJava;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import scala.Function0;
import scala.Option;
import scala.runtime.BoxedUnit;

/** Java implementation of the nanomachine controller tick. */
public final class ControllerTick {
    private ControllerTick() { }

    public static void update(ControllerImpl controller) {
        Player player = controller.player();
        if (!player.isAlive()) return;

        Level level = player.level();
        boolean server = !level.isClientSide();
        if (server) {
            if (controller.commandDelay() > 0) {
                controller.commandDelay_$eq(controller.commandDelay() - 1);
                if (controller.commandDelay() == 0) {
                    Option<Function0<BoxedUnit>> queued = controller.queuedCommand();
                    if (queued.isDefined()) queued.get().apply();
                    controller.queuedCommand_$eq(Option.empty());
                }
            }

            if (!level.dimension().equals(controller.previousDimension())) {
                Network.leaveWirelessNetwork(controller, controller.previousDimension());
                Network.joinWirelessNetwork(controller);
                controller.previousDimension_$eq(level.dimension());
            } else {
                Network.updateWirelessNetwork(controller);
            }
        }

        boolean hasPower = controller.getLocalBuffer() > 0 || SettingsAccessJava.get().ignorePower();
        Iterable<Behavior> active = null;
        int activeInputs = -1;
        if (hasPower != controller.hadPower()) {
            active = controller.getActiveBehaviors();
            if (!hasPower) {
                for (Behavior behavior : active) behavior.onDisable(DisableReason.OutOfEnergy);
                hasPower = controller.getLocalBuffer() > 0 || SettingsAccessJava.get().ignorePower();
            } else {
                for (Behavior behavior : active) behavior.onEnable();
            }
        }

        if (hasPower) {
            if (active == null) active = controller.getActiveBehaviors();
            for (Behavior behavior : active) behavior.update();

            level = player.level();
            server = !level.isClientSide();

            if (server) {
                if (level.getGameTime() % SettingsAccessJava.get().tickFrequency() == 0) {
                    activeInputs = countActiveInputs(controller);
                    controller.changeBuffer(-SettingsAccessJava.get().nanomachineCost() *
                            SettingsAccessJava.get().tickFrequency() * (activeInputs + 0.5));
                    NanomachinePacketSender.sendPowerUnchecked(player);
                }

                if (activeInputs < 0) activeInputs = countActiveInputs(controller);
                int overload = activeInputs - controller.getSafeActiveInputs();
                if (!player.isCreative() && overload > 0 && level.getGameTime() % 20 == 0) {
                    var source = new li.cil.oc.integration.util.DamageSourceWithRandomCause(
                            ControllerImplConstants.OverloadDamageKey, 3, level);
                    player.hurt(source, (float) overload);
                }
            }

            if (!server && SettingsAccessJava.get().enableNanomachinePfx()) {
                if (activeInputs < 0) activeInputs = countActiveInputs(controller);
                double energyRatio = controller.getLocalBuffer() / (controller.getLocalBufferSize() + 1);
                double triggerRatio = (double) activeInputs /
                        (controller.configuration().triggers.length() + 1);
                PlayerUtils.spawnParticleAround(player, ParticleTypes.PORTAL, (energyRatio + triggerRatio) * 0.25);
            }
        }

        // Behaviors may move the player, so query its current level for the
        // synchronization and side checks below just as the Scala tick did.
        level = player.level();
        server = !level.isClientSide();

        if (server) {
            if (controller.hadPower() != hasPower) NanomachinePacketSender.sendPowerUnchecked(player);
            if (!controller.hasSentConfiguration() || level.getGameTime() % controller.FullSyncInterval() == 0) {
                controller.hasSentConfiguration_$eq(true);
                NanomachinePacketSender.sendConfigurationUnchecked(player);
            }
        }

        controller.hadPower_$eq(hasPower);
    }

    private static int countActiveInputs(ControllerImpl controller) {
        int count = 0;
        var triggers = controller.configuration().triggers;
        for (int i = 0; i < triggers.length(); i++) if (triggers.apply(i).isActive()) count++;
        return count;
    }
}
