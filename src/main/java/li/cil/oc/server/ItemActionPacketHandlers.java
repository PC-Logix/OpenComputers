package li.cil.oc.server;

import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.common.item.TabletCache;
import li.cil.oc.common.item.TabletEvents;
import li.cil.oc.common.item.data.DriveData;
import li.cil.oc.common.item.traits.FileSystemLike;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

/** Server handlers for drive and tablet item actions. */
public final class ItemActionPacketHandlers {
    private ItemActionPacketHandlers() {}

    public static void onDriveLock(PacketParser packet) {
        if (packet.player instanceof ServerPlayer player) {
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (heldItem.getItem() instanceof FileSystemLike) DriveData.lock(heldItem, player);
        }
    }

    public static void onDriveMode(PacketParser packet) throws IOException {
        boolean unmanaged = packet.readBoolean();
        if (packet.player instanceof ServerPlayer player) {
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (heldItem.getItem() instanceof FileSystemLike) DriveData.setUnmanaged(heldItem, unmanaged);
        }
    }

    public static void onMachineItemStateRequest(PacketParser packet) throws IOException {
        if (packet.player instanceof ServerPlayer player) {
            ItemStack stack = packet.readItemStack();
            boolean running = TabletCache.get(stack, packet.player).machine().isRunning();
            GeneralPacketSender.sendMachineItemState(player, stack, running);
        }
    }

    public static void onTabletCursorTick(PacketParser packet) throws IOException {
        if (packet.player instanceof ServerPlayer player) TabletEvents.tickCursorHeld(player, packet.readUTF());
    }
}
