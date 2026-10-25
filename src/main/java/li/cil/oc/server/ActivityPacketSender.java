package li.cil.oc.server;

import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.common.blockentity.DiskDrive;
import li.cil.oc.common.blockentity.Relay;
import li.cil.oc.common.blockentity.Screen;
import li.cil.oc.common.blockentity.Transposer;
import li.cil.oc.common.blockentity.Waypoint;
import net.minecraft.world.item.ItemStack;
import java.io.IOException;
import java.util.Optional;

public final class ActivityPacketSender {
    private ActivityPacketSender() {}

    public static void sendFloppyChange(DiskDrive drive, ItemStack stack) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.FloppyChange);
        packet.writeTileEntity(drive);
        packet.writeItemStack(stack, drive.getLevel().registryAccess());
        packet.sendToPlayersNearTileEntity(drive);
    }

    public static void sendSwitchActivity(Relay relay, double range) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.SwitchActivity);
        packet.writeTileEntity(relay);
        packet.sendToPlayersNearTileEntity(relay, Optional.of(range));
    }

    public static void sendScreenTouchMode(Screen screen, boolean value) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.ScreenTouchMode);
        packet.writeTileEntity(screen);
        packet.writeBoolean(value);
        packet.sendToPlayersNearTileEntity(screen);
    }

    public static void sendScreenTouchModeUnchecked(Screen screen, boolean value) {
        try {
            sendScreenTouchMode(screen, value);
        } catch (IOException error) {
            ActivityPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendTransposerActivity(Transposer transposer, double range) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.TransposerActivity);
        packet.writeTileEntity(transposer);
        packet.sendToPlayersNearTileEntity(transposer, Optional.of(range));
    }

    public static void sendTransposerActivityUnchecked(Transposer transposer, double range) {
        try {
            sendTransposerActivity(transposer, range);
        } catch (IOException error) {
            ActivityPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendWaypointLabel(Waypoint waypoint) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.WaypointLabel);
        packet.writeTileEntity(waypoint);
        packet.writeUTF(waypoint.label());
        packet.sendToPlayersNearTileEntity(waypoint);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }
}
