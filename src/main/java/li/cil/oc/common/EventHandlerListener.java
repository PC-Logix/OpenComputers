package li.cil.oc.common;

import li.cil.oc.Localization;
import li.cil.oc.client.renderer.PetRenderer;
import li.cil.oc.common.blockentity.Case;
import li.cil.oc.common.blockentity.RobotProxy;
import li.cil.oc.common.component.TerminalServer;

import li.cil.oc.server.LootPacketSender;
import li.cil.oc.server.PetVisibilityPacketSender;


import li.cil.oc.server.machine.luac.LuaStateFactory;
import li.cil.oc.util.SideTracker;
import li.cil.oc.util.UpdateCheck;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.io.IOException;

import java.util.Optional;


/** Registered NeoForge listener for world, player, and tick lifecycle events. */
public final class EventHandlerListener {
    public static final EventHandlerListener INSTANCE = new EventHandlerListener();

    private EventHandlerListener() {}

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    private static void runUnchecked(IoAction action) {
        try {
            action.run();
        } catch (IOException error) {
            throw sneakyThrow(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(Throwable error) throws E {
        throw (E) error;
    }

    @SubscribeEvent
    public void onServerTickPre(ServerTickEvent.Pre event) {
        ScheduledEvents.onServerTickPre(event);
    }

    @SubscribeEvent
    public void onServerTickPost(ServerTickEvent.Post event) {
        ScheduledEvents.onServerTickPost();
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        ScheduledEvents.onClientTickPre();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!SideTracker.isServer() || event.getEntity() instanceof FakePlayer || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (!LuaStateFactory.isAvailable() && !LuaStateFactory.luajRequested()) {
            player.sendSystemMessage(Localization.Chat.WarningLuaFallback());
        }
        EventHandler.scheduleServer(() -> {
            runUnchecked(() -> PetVisibilityPacketSender.send(Optional.empty(), Optional.of(player)));
            runUnchecked(() -> LootPacketSender.sendDisks(player));
            runUnchecked(() -> LootPacketSender.sendEeproms(player));
            return scala.runtime.BoxedUnit.UNIT;
        });
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server.getPlayerList().isOp(player.getGameProfile())) {
            UpdateCheck.info.thenAccept(release -> release.ifPresent(value ->
                player.sendSystemMessage(Localization.Chat.InfoNewVersion(value.tag_name))));
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        li.cil.oc.client.PacketHandler.clearPendingProjectorFrames();
        li.cil.oc.client.PacketHandler.stopAllAudio();
        PetRenderer.isInitialized = false;
        PetRenderer.hidden.clear();
        Loot.resetDisksForClient();
        Loot.disksForCyclingClient.clear();
        scala.collection.Iterator<ItemStack> disks = Loot.disksForCyclingServer.iterator();
        while (disks.hasNext()) Loot.disksForCyclingClient.append(disks.next().copy());
        Loot.eepromsForClient.clear();
        li.cil.oc.client.Sound.stopAll();
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        li.cil.oc.client.PacketHandler.stopAllAudio();
        li.cil.oc.client.Sound.stopAll();
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        var entity = event.getLevel().getBlockEntity(event.getPos());
        Player player = event.getPlayer();
        if (entity instanceof Case machineCase && machineCase.isCreative()
                && (!player.isCreative() || !machineCase.canInteract(player.getName().getString()))) {
            event.setCanceled(true);
        } else if (entity instanceof RobotProxy proxy) {
            var robot = proxy.robot();
            if (robot.isCreative() && (!player.isCreative() || !robot.canInteract(player.getName().getString()))) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ScheduledEvents.releaseKeyboardKeys(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ScheduledEvents.releaseKeyboardKeys(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ScheduledEvents.releaseKeyboardKeys(event.getEntity());
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        PlayerItemEvents.onEntityJoinLevel(event);
    }

    @SubscribeEvent
    public void onCrafting(PlayerEvent.ItemCraftedEvent event) {
        PlayerItemEvents.onCrafting(event);
    }

    @SubscribeEvent
    public void onItemEntityPickup(ItemEntityPickupEvent.Post event) {
        PlayerItemEvents.onItemEntityPickup(event);
    }

    @SubscribeEvent
    public synchronized void onWorldUnload(LevelEvent.Unload event) {
        var level = event.getLevel();
        if (level.isClientSide()) {
            li.cil.oc.client.PacketHandler.stopAllAudio();
            TerminalServer.loaded.clear();
            return;
        }
        WorldLifecycleEvents.disposeServerWorld((ServerLevel) level);
    }

    @SubscribeEvent
    public void onChunkUnloaded(ChunkEvent.Unload event) {
        ChunkLifecycleEvents.onChunkUnloaded(event);
    }

    @SubscribeEvent
    public void onChunkSent(ChunkWatchEvent.Sent event) {
        runUnchecked(() -> ChunkLifecycleEvents.onChunkSent(event));
    }
}
