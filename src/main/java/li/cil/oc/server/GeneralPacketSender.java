package li.cil.oc.server;

import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.common.CompressedPacketBuilder;
import li.cil.oc.common.blockentity.Adapter;
import li.cil.oc.common.blockentity.Charger;
import li.cil.oc.common.blockentity.Disassembler;
import li.cil.oc.common.blockentity.traits.Colored;
import li.cil.oc.common.blockentity.traits.Computer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.IOException;

public final class GeneralPacketSender {
    private GeneralPacketSender() {}

    public static void sendAdapterState(Adapter tile) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.AdapterState);
        packet.writeTileEntity(tile);
        packet.writeByte(tile.compressSides());
        packet.sendToPlayersNearTileEntity(tile, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendAnalyze(String address, ServerPlayer player) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.Analyze);
        packet.writeUTF(address);
        packet.sendToPlayer(player);
    }

    public static void sendAnalyzeUnchecked(String address, ServerPlayer player) {
        try {
            sendAnalyze(address, player);
        } catch (IOException error) {
            GeneralPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendChargerState(Charger tile) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.ChargerState);
        packet.writeTileEntity(tile);
        packet.writeDouble(tile.chargeSpeed());
        packet.writeBoolean(tile.hasPower());
        packet.sendToPlayersNearTileEntity(tile, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendChargerStateUnchecked(Charger tile) {
        try {
            sendChargerState(tile);
        } catch (IOException error) {
            GeneralPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendClientLog(String line, ServerPlayer player) throws IOException {
        CompressedPacketBuilder packet = new CompressedPacketBuilder(PacketType.ClientLog, CompressedPacketBuilder.$lessinit$greater$default$2());
        packet.writeUTF(line);
        packet.sendToPlayer(player);
    }

    public static void sendClientLogUnchecked(String line, ServerPlayer player) {
        try {
            sendClientLog(line, player);
        } catch (IOException error) {
            GeneralPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendClipboard(ServerPlayer player, String text) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.Clipboard);
        packet.writeUTF(text);
        packet.sendToPlayer(player);
    }

    public static void sendColorChange(Colored tile) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.ColorChange);
        packet.writeTileEntity((BlockEntity) tile);
        packet.writeInt(tile.getColor());
        packet.sendToPlayersNearTileEntity((BlockEntity) tile, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendComputerState(Computer tile) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.ComputerState);
        packet.writeTileEntity((BlockEntity) tile);
        packet.writeBoolean(tile.isRunning());
        packet.writeBoolean(tile.hasErrored());
        packet.sendToPlayersNearTileEntity((BlockEntity) tile, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendMachineItemState(ServerPlayer player, ItemStack stack, boolean running) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.MachineItemStateResponse);
        packet.writeItemStack(stack, player.server.registryAccess());
        packet.writeBoolean(running);
        packet.sendToPlayer(player);
    }

    public static void sendComputerUserListUnchecked(Computer tile, String[] names) {
        try {
            sendComputerUserList(tile, names);
        } catch (IOException error) {
            GeneralPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendContainerUpdateUnchecked(AbstractContainerMenu container, CompoundTag nbt, ServerPlayer player) {
        try {
            sendContainerUpdate(container, nbt, player);
        } catch (IOException error) {
            GeneralPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendComputerUserList(Computer tile, String[] names) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.ComputerUserList);
        packet.writeTileEntity((BlockEntity) tile);
        packet.writeInt(names.length);
        for (String name : names) packet.writeUTF(name);
        packet.sendToPlayersNearTileEntity((BlockEntity) tile, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendContainerUpdate(AbstractContainerMenu container, CompoundTag nbt, ServerPlayer player) throws IOException {
        if (!nbt.isEmpty()) {
            SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.ContainerUpdate);
            packet.writeInt(container.containerId);
            packet.writeNBT(nbt);
            packet.sendToPlayer(player);
        }
    }

    public static void sendMachineItemStateUnchecked(ServerPlayer player, ItemStack stack, boolean running) {
        try {
            sendMachineItemState(player, stack, running);
        } catch (IOException error) {
            GeneralPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendDisassemblerActive(Disassembler tile, boolean active) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.DisassemblerActiveChange);
        packet.writeTileEntity(tile);
        packet.writeBoolean(active);
        packet.sendToPlayersNearTileEntity(tile, packet.sendToPlayersNearTileEntity$default$2());
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }
}
