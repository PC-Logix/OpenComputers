package li.cil.oc.client;

import li.cil.oc.Settings;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.common.blockentity.HoloScreen;
import li.cil.oc.common.blockentity.Rack;
import li.cil.oc.common.blockentity.Waypoint;
import li.cil.oc.common.menu.Assembler;
import li.cil.oc.common.menu.Server;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import scala.Option;
import scala.Option$;

import java.io.IOException;

public final class PacketSender {
    private PacketSender() {}

    public static void sendMachineItemStateRequest(ItemStack stack, RegistryAccess registryAccess) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.MachineItemStateRequest);
            packet.writeItemStack(stack, registryAccess);
            packet.sendToServer();
        });
    }

    public static void sendTabletCursorTick(String id) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.TabletCursorTick);
            packet.writeUTF(id);
            packet.sendToServer();
        });
    }

    public static void sendMultiPlace() {
        send(() -> new SimplePacketBuilder(PacketType.MultiPartPlace).sendToServer());
    }

    public static void sendPetVisibility() {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.PetVisibility);
            packet.writeBoolean(!Settings.get().hideOwnPet());
            packet.sendToServer();
        });
    }

    public static void sendRackMountableMapping(li.cil.oc.common.menu.Rack rack, int mountableIndex,
                                                int nodeIndex, Option<Direction> side) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RackMountableMapping);
            packet.writeInt(rack.containerId);
            packet.writeInt(mountableIndex);
            packet.writeInt(nodeIndex);
            packet.writeDirection(side);
            packet.sendToServer();
        });
    }

    public static void sendRackRelayState(li.cil.oc.common.menu.Rack rack, boolean enabled) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RackRelayState);
            packet.writeInt(rack.containerId);
            packet.writeBoolean(enabled);
            packet.sendToServer();
        });
    }

    public static void sendRackKVMSelection(Rack rack, int kvmSlot, int serverSlot) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RackKVMSelection);
            packet.writeTileEntity(rack);
            packet.writeInt(kvmSlot);
            packet.writeInt(serverSlot);
            packet.sendToServer();
        });
    }

    public static void sendRobotAssemblerStart(Assembler assembler) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RobotAssemblerStart);
            packet.writeInt(assembler.containerId);
            packet.sendToServer();
        });
    }

    public static void sendRobotStateRequest(ResourceLocation dimension, int x, int y, int z) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RobotStateRequest);
            packet.writeUTF(dimension.toString());
            packet.writeInt(x);
            packet.writeInt(y);
            packet.writeInt(z);
            packet.sendToServer();
        });
    }

    public static void sendServerPower(Server server, int mountableIndex, boolean power) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.ServerPower);
            packet.writeInt(server.containerId);
            packet.writeInt(mountableIndex);
            packet.writeBoolean(power);
            packet.sendToServer();
        });
    }

    public static void sendWaypointLabel(Waypoint waypoint) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.WaypointLabel);
            packet.writeTileEntity(waypoint);
            packet.writeUTF(waypoint.label());
            packet.sendToServer();
        });
    }

    public static void sendHoloScreenResize(HoloScreen screen, Direction side) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.HoloScreenResize);
            packet.writeTileEntity(screen);
            packet.writeDirection(Option$.MODULE$.apply(side));
            packet.sendToServer();
        });
    }

    private static void send(PacketAction action) {
        try {
            action.send();
        } catch (IOException error) {
            PacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }

    @FunctionalInterface
    private interface PacketAction {
        void send() throws IOException;
    }
}
