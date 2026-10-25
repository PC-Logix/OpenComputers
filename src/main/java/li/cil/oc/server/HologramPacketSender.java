package li.cil.oc.server;

import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.common.CompressedPacketBuilder;
import li.cil.oc.common.blockentity.Hologram;
import java.io.IOException;

public final class HologramPacketSender {
    private HologramPacketSender() {}

    public static void sendClear(Hologram hologram) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.HologramClear);
        packet.writeTileEntity(hologram);
        packet.sendToPlayersNearTileEntity(hologram, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendColor(Hologram hologram, int index, int value) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.HologramColor);
        packet.writeTileEntity(hologram);
        packet.writeInt(index);
        packet.writeInt(value);
        packet.sendToPlayersNearTileEntity(hologram, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendPowerChange(Hologram hologram) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.HologramPowerChange);
        packet.writeTileEntity(hologram);
        packet.writeBoolean(hologram.hasPower());
        packet.sendToPlayersNearTileEntity(hologram, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendScale(Hologram hologram) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.HologramScale);
        packet.writeTileEntity(hologram);
        packet.writeDouble(hologram.scale());
        packet.sendToPlayersNearTileEntity(hologram, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendArea(Hologram hologram) throws IOException {
        CompressedPacketBuilder packet = new CompressedPacketBuilder(PacketType.HologramArea, CompressedPacketBuilder.$lessinit$greater$default$2());
        packet.writeTileEntity(hologram);
        packet.writeByte(hologram.dirtyFromX());
        packet.writeByte(hologram.dirtyUntilX());
        packet.writeByte(hologram.dirtyFromZ());
        packet.writeByte(hologram.dirtyUntilZ());
        for (int x = hologram.dirtyFromX(); x < hologram.dirtyUntilX(); x++) {
            for (int z = hologram.dirtyFromZ(); z < hologram.dirtyUntilZ(); z++) {
                packet.writeInt(hologram.volume()[x + z * hologram.width()]);
                packet.writeInt(hologram.volume()[x + z * hologram.width() + hologram.width() * hologram.width()]);
            }
        }
        packet.sendToPlayersNearTileEntity(hologram, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendValues(Hologram hologram) throws IOException {
        CompressedPacketBuilder packet = new CompressedPacketBuilder(PacketType.HologramValues, CompressedPacketBuilder.$lessinit$greater$default$2());
        packet.writeTileEntity(hologram);
        packet.writeInt(hologram.dirty().size());
        scala.collection.Iterator<Object> iterator = hologram.dirty().iterator();
        while (iterator.hasNext()) {
            int xz = ((Number) iterator.next()).intValue();
            byte x = (byte) (xz >> 8);
            byte z = (byte) xz;
            packet.writeShort(xz);
            int rangeStart = x + z * hologram.width();
            int rangeFinal = x + z * hologram.width() + hologram.width() * hologram.width();
            packet.writeInt(hologram.volume()[Math.min(Math.max(rangeStart, 0), hologram.volume().length - 1)]);
            packet.writeInt(hologram.volume()[Math.min(Math.max(rangeFinal, 0), hologram.volume().length - 1)]);
        }
        packet.sendToPlayersNearTileEntity(hologram, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendOffset(Hologram hologram) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.HologramTranslation);
        packet.writeTileEntity(hologram);
        packet.writeDouble(hologram.translation().x);
        packet.writeDouble(hologram.translation().y);
        packet.writeDouble(hologram.translation().z);
        packet.sendToPlayersNearTileEntity(hologram, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendRotation(Hologram hologram) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.HologramRotation);
        packet.writeTileEntity(hologram);
        packet.writeFloat(hologram.rotationAngle());
        packet.writeFloat(hologram.rotationX());
        packet.writeFloat(hologram.rotationY());
        packet.writeFloat(hologram.rotationZ());
        packet.sendToPlayersNearTileEntity(hologram, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendRotationSpeed(Hologram hologram) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.HologramRotationSpeed);
        packet.writeTileEntity(hologram);
        packet.writeFloat(hologram.rotationSpeed());
        packet.writeFloat(hologram.rotationSpeedX());
        packet.writeFloat(hologram.rotationSpeedY());
        packet.writeFloat(hologram.rotationSpeedZ());
        packet.sendToPlayersNearTileEntity(hologram, packet.sendToPlayersNearTileEntity$default$2());
    }
}
