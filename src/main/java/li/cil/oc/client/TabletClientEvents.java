package li.cil.oc.client;

import li.cil.oc.common.item.TabletCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Client cursor updates stay outside the common event subscriber's classloading boundary. */
@OnlyIn(Dist.CLIENT)
public final class TabletClientEvents {
    public static final TabletClientEvents INSTANCE = new TabletClientEvents();
    private TabletClientEvents() { }
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        TabletCache.client().cleanUp();
        Player player = Minecraft.getInstance().player;
        if (player != null) TabletCache.getIdOptional(player.containerMenu.getCarried()).ifPresent(PacketSender::sendTabletCursorTick);
        if (ServerLifecycleHooks.getCurrentServer() instanceof IntegratedServer && Minecraft.getInstance().isPaused()) {
            TabletCache.client().keepAlive();
            TabletCache.server().keepAlive();
        }
    }
}
