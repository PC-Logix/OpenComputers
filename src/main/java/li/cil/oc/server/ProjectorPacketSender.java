package li.cil.oc.server;

import li.cil.oc.common.CompressedPacketBuilder;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.common.blockentity.Projector;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

public final class ProjectorPacketSender {
    private ProjectorPacketSender() {}

    public static void sendPowerChange(Projector projector) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.ProjectorPowerChange);
        packet.writeTileEntity(projector);
        packet.writeBoolean(projector.isOn());
        packet.writeBoolean(projector.hasPower());
        packet.sendToPlayersNearTileEntity(projector, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendFrame(Projector projector, int fromX, int fromY, int untilX, int untilY) throws IOException {
        CompressedPacketBuilder packet = framePacket(projector, fromX, fromY, untilX, untilY);
        packet.sendToPlayersNearTileEntity(projector, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendFrameToPlayer(Projector projector, ServerPlayer player) throws IOException {
        framePacket(projector, 0, 0, projector.width(), projector.height()).sendToPlayer(player);
    }

    private static CompressedPacketBuilder framePacket(Projector projector, int fromX, int fromY, int untilX, int untilY) throws IOException {
        CompressedPacketBuilder packet = new CompressedPacketBuilder(
                PacketType.ProjectorFrame, CompressedPacketBuilder.$lessinit$greater$default$2());
        packet.writeTileEntity(projector);
        packet.writeInt(fromX);
        packet.writeInt(fromY);
        packet.writeInt(untilX - fromX);
        packet.writeInt(untilY - fromY);
        for (int y = fromY; y < untilY; y++) {
            for (int x = fromX; x < untilX; x++) packet.writeInt(projector.pixels()[x + y * projector.width()]);
        }
        return packet;
    }
}
