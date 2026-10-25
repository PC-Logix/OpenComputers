package li.cil.oc.client;

import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.common.menu.Case;
import li.cil.oc.common.menu.Drone;
import li.cil.oc.common.menu.Robot;
import li.cil.oc.common.menu.Tablet;
import li.cil.oc.common.menu.TapeDrive;

import java.io.IOException;

public final class MenuClientPacketSender {
    private MenuClientPacketSender() {}

    public static void sendComputerPower(Case computer, boolean power) {
        sendContainerPower(computer.containerId, power);
    }

    public static void sendRobotPower(Robot robot, boolean power) {
        sendContainerPower(robot.containerId, power);
    }

    public static void sendTabletPower(Tablet tablet, boolean power) {
        sendContainerPower(tablet.containerId, power);
    }

    private static void sendContainerPower(int containerId, boolean power) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.ComputerPower);
            packet.writeInt(containerId);
            packet.writeBoolean(power);
            packet.sendToServer();
        });
    }

    public static void sendDriveMode(boolean unmanaged) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.DriveMode);
            packet.writeBoolean(unmanaged);
            packet.sendToServer();
        });
    }

    public static void sendDriveLock() {
        send(() -> new SimplePacketBuilder(PacketType.DriveLock).sendToServer());
    }

    public static void sendDronePower(Drone drone, boolean power) {
        send(() -> sendContainerPowerPacket(drone.containerId, power));
    }

    private static void sendContainerPowerPacket(int containerId, boolean power) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.DronePower);
        packet.writeInt(containerId);
        packet.writeBoolean(power);
        packet.sendToServer();
    }

    public static void sendTapeDriveControl(TapeDrive drive, int action) {
        send(() -> {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.TapeDriveControl);
            packet.writeInt(drive.containerId);
            packet.writeByte((byte) action);
            packet.sendToServer();
        });
    }

    private static void send(PacketAction action) {
        try {
            action.send();
        } catch (IOException error) {
            MenuClientPacketSender.<RuntimeException>throwUnchecked(error);
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
