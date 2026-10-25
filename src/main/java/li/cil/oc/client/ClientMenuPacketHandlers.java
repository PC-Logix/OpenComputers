package li.cil.oc.client;

import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.common.item.TabletCache;
import li.cil.oc.common.item.TabletWrapper;
import li.cil.oc.common.menu.AbstractMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

/** Applies clientbound tablet and open-menu state updates. */
public final class ClientMenuPacketHandlers {
    private ClientMenuPacketHandlers() {}

    public static void machineItemState(PacketParser packet) throws IOException {
        ItemStack stack = packet.readItemStack();
        boolean running = packet.readBoolean();
        TabletWrapper wrapper = TabletCache.client().get(stack, packet.player);
        wrapper.data.isRunning = running;
        wrapper.isDirty = false;
    }

    public static void containerUpdate(PacketParser packet) throws IOException {
        int containerId = packet.readInt();
        AbstractContainerMenu activeMenu = packet.player.containerMenu;
        if (activeMenu != null && activeMenu.containerId == containerId && activeMenu instanceof AbstractMenu menu) {
            menu.updateCustomData(packet.readNBT());
        }
    }
}
