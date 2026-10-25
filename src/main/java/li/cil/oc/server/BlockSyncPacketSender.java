package li.cil.oc.server;

import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.common.blockentity.NetSplitter;
import li.cil.oc.common.blockentity.Printer;
import li.cil.oc.common.blockentity.traits.PowerInformation;
import li.cil.oc.common.blockentity.traits.RedstoneAware;
import li.cil.oc.common.blockentity.traits.Rotatable;
import net.minecraft.core.Direction;

import java.io.IOException;

public final class BlockSyncPacketSender {
    private BlockSyncPacketSender() {}

    public static void sendNetSplitter(NetSplitter splitter) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.NetSplitterState);
        packet.writeTileEntity(splitter);
        packet.writeBoolean(splitter.isInverted());
        packet.writeByte(splitter.compressSides());
        packet.sendToPlayersNearTileEntity(splitter, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendPowerState(PowerInformation tile) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.PowerState);
        packet.writeTileEntity((net.minecraft.world.level.block.entity.BlockEntity) tile);
        packet.writeDouble((double) Math.round(tile.globalBuffer()));
        packet.writeDouble(tile.globalBufferSize());
        packet.sendToPlayersNearTileEntity((net.minecraft.world.level.block.entity.BlockEntity) tile,
                packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendPrinting(Printer printer, boolean printing) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.PrinterState);
        packet.writeTileEntity(printer);
        packet.writeBoolean(printing);
        packet.sendToPlayersNearHost(printer, packet.sendToPlayersNearHost$default$2());
    }

    public static void sendRedstone(RedstoneAware tile) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RedstoneState);
        packet.writeTileEntity((net.minecraft.world.level.block.entity.BlockEntity) tile);
        packet.writeBoolean(tile.isOutputEnabled());
        for (Direction direction : Direction.values()) packet.writeByte(tile.getOutput(direction));
        packet.sendToPlayersNearTileEntity((net.minecraft.world.level.block.entity.BlockEntity) tile,
                packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendRotatable(Rotatable tile) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RotatableState);
        packet.writeTileEntity((net.minecraft.world.level.block.entity.BlockEntity) tile);
        packet.writeByte(tile.pitch().ordinal());
        packet.writeByte(tile.yaw().ordinal());
        packet.sendToPlayersNearTileEntity((net.minecraft.world.level.block.entity.BlockEntity) tile,
                packet.sendToPlayersNearTileEntity$default$2());
    }
}
