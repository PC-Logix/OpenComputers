package li.cil.oc.client;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import io.netty.buffer.Unpooled;
import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.common.blockentity.Rack;
import li.cil.oc.common.component.RemoteTerminalHost;
import li.cil.oc.common.datacomponents.CompoundStorage;
import li.cil.oc.util.ClientAccessHelper;
import li.cil.oc.util.ExtendedLevelJava;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.connection.ConnectionType;
import java.io.IOException;
import java.util.Optional;

/** Applies clientbound rack inventory and mountable data updates. */
public final class ClientRackPacketHandlers {
    private ClientRackPacketHandlers() {}

    public static void inventory(PacketParser packet) throws IOException {
        Optional<Rack> selectedRack = packet.readBlockEntityOptional(Rack.class);
        if (selectedRack.isEmpty()) return;

        Rack rack = selectedRack.get();
        int count = packet.readInt();
        for (int index = 0; index < count; index++) {
            int slot = packet.readInt();
            ItemStack incoming = packet.readItemStack();
            ItemStack current = rack.getItem(slot);
            String currentAddress = current.isEmpty() ? null : current.get(OCComponentsJava.ADDRESS.get());
            String incomingAddress = incoming.isEmpty() ? null : incoming.get(OCComponentsJava.ADDRESS.get());

            // A persistence-only update to the same mountable must keep its live remote terminal environment.
            boolean preserveRemoteEnvironment = slot >= 0 && slot < rack.getContainerSize()
                && !current.isEmpty() && !incoming.isEmpty()
                && ItemStack.isSameItem(current, incoming)
                && currentAddress != null && currentAddress.equals(incomingAddress)
                && rack.getMountable(slot) instanceof RemoteTerminalHost;

            if (preserveRemoteEnvironment) rack.updateItems(slot, incoming);
            else rack.setItem(slot, incoming);
        }
    }

    public static void mountableData(PacketParser packet) throws IOException {
        Optional<Rack> selectedRack = packet.readBlockEntityOptional(Rack.class);
        if (selectedRack.isEmpty()) return;

        Rack rack = selectedRack.get();
        int mountableIndex = packet.readInt();
        RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(
            Unpooled.wrappedBuffer(packet.readAllBytes()),
            ClientAccessHelper.getClientRegistryAccess(),
            ConnectionType.NEOFORGE);
        rack.lastData()[mountableIndex] = CompoundStorage.OPTION_STREAM_CODEC.decode(data);
        new ExtendedLevelJava(rack.getLevel()).notifyBlockUpdate(rack.getBlockPos());
    }
}
