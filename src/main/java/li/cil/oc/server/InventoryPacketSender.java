package li.cil.oc.server;

import io.netty.buffer.Unpooled;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.common.blockentity.Rack;
import li.cil.oc.common.blockentity.Raid;
import li.cil.oc.common.datacomponents.CompoundStorage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;

public final class InventoryPacketSender {
    private InventoryPacketSender() {}

    public static void sendRackInventory(Rack rack) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RackInventory);
        packet.writeTileEntity(rack);
        packet.writeInt(rack.getContainerSize());
        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
            packet.writeInt(slot);
            packet.writeItemStack(rack.getItem(slot), rack.getLevel().registryAccess());
        }
        packet.sendToPlayersNearTileEntity(rack, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendRackInventory(Rack rack, int slot) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RackInventory);
        packet.writeTileEntity(rack);
        packet.writeInt(1);
        packet.writeInt(slot);
        packet.writeItemStack(rack.getItem(slot), rack.getLevel().registryAccess());
        packet.sendToPlayersNearTileEntity(rack, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendRackMountableData(Rack rack, int mountable) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RackMountableData);
        packet.writeTileEntity(rack);
        packet.writeInt(mountable);
        RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), ServerLifecycleHooks.getCurrentServer().registryAccess(), ConnectionType.NEOFORGE);
        CompoundStorage.OPTION_STREAM_CODEC.encode(data, rack.lastData()[mountable]);
        packet.write(data.array());
        packet.sendToPlayersNearTileEntity(rack, packet.sendToPlayersNearTileEntity$default$2());
    }

    public static void sendRaidChange(Raid raid) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.RaidStateChange);
        packet.writeTileEntity(raid);
        for (int slot = 0; slot < raid.getContainerSize(); slot++) packet.writeBoolean(!raid.getItem(slot).isEmpty());
        packet.sendToPlayersNearTileEntity(raid, packet.sendToPlayersNearTileEntity$default$2());
    }
}
