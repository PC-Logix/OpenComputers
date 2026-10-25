package li.cil.oc.client;

import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.common.nanomachines.ControllerImpl;
import li.cil.oc.common.nanomachines.NeuralNetwork;
import net.minecraft.world.entity.player.Player;
import java.io.IOException;
import java.util.Optional;

/** Applies clientbound nanomachine controller updates. */
public final class ClientNanomachinePacketHandlers {
    private ClientNanomachinePacketHandlers() {}

    public static void configuration(PacketParser packet) throws IOException {
        Optional<Player> playerValue = packet.readEntityOptional(Player.class);
        if (playerValue.isEmpty()) return;

        Player player = playerValue.get();
        if (packet.readBoolean()) {
            Controller installed = Nanomachines.installController(player);
            if (installed instanceof ControllerImpl controller) controller.loadData(packet.readNBT());
        } else {
            Nanomachines.uninstallController(player);
        }
    }

    public static void inputs(PacketParser packet) throws IOException {
        Optional<Player> playerValue = packet.readEntityOptional(Player.class);
        if (playerValue.isEmpty()) return;
        Controller value = Nanomachines.getController(playerValue.get());
        if (!(value instanceof ControllerImpl controller)) return;

        byte[] inputs = new byte[packet.readInt()];
        packet.read(inputs);
        NeuralNetwork configuration = controller.configuration();
        synchronized (configuration) {
            for (int index = 0; index < inputs.length && index < configuration.triggers.length(); index++) {
                configuration.triggers.apply(index).isActive_$eq(inputs[index] == 1);
            }
            controller.activeBehaviorsDirty_$eq(true);
        }
    }

    public static void power(PacketParser packet) throws IOException {
        Optional<Player> playerValue = packet.readEntityOptional(Player.class);
        if (playerValue.isPresent()) {
            Controller value = Nanomachines.getController(playerValue.get());
            if (value instanceof ControllerImpl controller) controller.storedEnergy_$eq(packet.readDouble());
        }
    }
}
