package li.cil.oc.server;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.common.component.RackKVM;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import java.io.IOException;
import java.util.Optional;

/** Server handlers for rack menu connection, relay, and KVM controls. */
public final class RackPacketHandlers {
    private static final Marker SECURITY_MARKER = MarkerManager.getMarker("SuspiciousPackets");

    private RackPacketHandlers() {}

    public static void onRackMountableMapping(PacketParser packet) throws IOException {
        int containerId = packet.readInt();
        int mountableIndex = packet.readInt();
        int nodeIndex = packet.readInt();
        Optional<net.minecraft.core.Direction> side = packet.readDirectionOptional();
        if (!(packet.player instanceof ServerPlayer player)) return;

        if (player.containerMenu instanceof li.cil.oc.common.menu.Rack menu && menu.containerId == containerId) {
            if (menu.otherInventory() instanceof li.cil.oc.common.blockentity.Rack rack) {
                if (rack.stillValid(player)) rack.connect(mountableIndex, nodeIndex - 1, side);
                // Otherwise the client may simply be lagging behind.
            } else logForgedPacket(player);
        } else logForgedPacket(player);
    }

    public static void onRackRelayState(PacketParser packet) throws IOException {
        int containerId = packet.readInt();
        boolean enabled = packet.readBoolean();
        if (packet.player.containerMenu instanceof li.cil.oc.common.menu.Rack menu && menu.containerId == containerId) {
            if (menu.otherInventory() instanceof li.cil.oc.common.blockentity.Rack rack &&
                    packet.player instanceof ServerPlayer player && rack.stillValid(player)) {
                rack.isRelayEnabled_$eq(enabled);
                rack.setChanged();
            }
        }
    }

    public static void onRackKVMSelection(PacketParser packet) throws IOException {
        Optional<li.cil.oc.common.blockentity.Rack> rack = packet.readBlockEntityOptional(li.cil.oc.common.blockentity.Rack.class);
        int kvmSlot = packet.readInt();
        int serverSlot = packet.readInt();
        if (!(packet.player instanceof ServerPlayer player) || rack.isEmpty()) return;

        li.cil.oc.common.blockentity.Rack targetRack = rack.get();
        if (kvmSlot < 0 || kvmSlot >= targetRack.getContainerSize()) return;
        RackMountable mountable = targetRack.getMountable(kvmSlot);
        if (mountable instanceof RackKVM kvm && kvm.isRemoteUsable(player)) kvm.selectRackSlot(serverSlot);
        else logForgedPacket(player);
    }

    private static void logForgedPacket(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            OpenComputers.log.warn(SECURITY_MARKER, "Player {} tried to send GUI packets without opening them", serverPlayer.getGameProfile());
        }
    }
}
