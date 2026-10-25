package li.cil.oc.server;

import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.common.nanomachines.ControllerImpl;
import li.cil.oc.common.nanomachines.NeuralNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import scala.Option$;

import java.io.IOException;

public final class NanomachinePacketSender {
    private NanomachinePacketSender() {}

    public static void sendConfiguration(Player player) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.NanomachinesConfiguration);
        packet.writeEntity(player);
        Controller value = Nanomachines.getController(player);
        if (value instanceof ControllerImpl controller) {
            packet.writeBoolean(true);
            CompoundTag nbt = new CompoundTag();
            controller.saveData(nbt);
            packet.writeNBT(nbt);
        } else {
            packet.writeBoolean(false);
        }
        packet.sendToPlayersNearEntity(player, Option$.MODULE$.empty());
    }

    public static void sendInputs(Player player) throws IOException {
        Controller value = Nanomachines.getController(player);
        if (!(value instanceof ControllerImpl controller)) return;

        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.NanomachinesInputs);
        packet.writeEntity(player);
        scala.collection.Iterator<NeuralNetwork.TriggerNeuron> triggers = controller.configuration().triggers.iterator();
        byte[] inputs = new byte[controller.configuration().triggers.size()];
        int index = 0;
        while (triggers.hasNext()) inputs[index++] = (byte) (triggers.next().isActive() ? 1 : 0);
        packet.writeInt(inputs.length);
        packet.write(inputs);
        packet.sendToPlayersNearEntity(player, Option$.MODULE$.empty());
    }

    public static void sendPower(Player player) throws IOException {
        Controller value = Nanomachines.getController(player);
        if (!(value instanceof ControllerImpl controller)) return;

        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.NanomachinesPower);
        packet.writeEntity(player);
        packet.writeDouble(controller.getLocalBuffer());
        packet.sendToPlayersNearEntity(player, Option$.MODULE$.empty());
    }

    public static void sendConfigurationUnchecked(Player player) {
        try {
            sendConfiguration(player);
        } catch (IOException error) {
            NanomachinePacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendInputsUnchecked(Player player) {
        try {
            sendInputs(player);
        } catch (IOException error) {
            NanomachinePacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendPowerUnchecked(Player player) {
        try {
            sendPower(player);
        } catch (IOException error) {
            NanomachinePacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }
}
