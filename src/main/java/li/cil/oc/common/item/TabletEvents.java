package li.cil.oc.common.item;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;
import scala.Tuple5;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

/** Common tablet cache lifecycle events and shared analysis state. */
public final class TabletEvents {
    public static final TabletEvents INSTANCE = new TabletEvents();

    private static Optional<Tuple5<li.cil.oc.util.BlockPosition, Direction, Float, Float, Float>> currentlyAnalyzing =
        Optional.empty();
    private static final Map<UUID, CursorTick> LAST_CURSOR_TICKS = new HashMap<>();

    private TabletEvents() { }

    public static Option<Tuple5<li.cil.oc.util.BlockPosition, Direction, Float, Float, Float>> currentlyAnalyzing() {
        return OptionConverters.toScala(currentlyAnalyzing);
    }

    public static Optional<Tuple5<li.cil.oc.util.BlockPosition, Direction, Float, Float, Float>> currentlyAnalyzingOptional() {
        return currentlyAnalyzing;
    }

    public static void startAnalysis(li.cil.oc.util.BlockPosition position, Direction face, float x, float y, float z) {
        currentlyAnalyzing = Optional.of(new Tuple5<>(position, face, x, y, z));
    }

    @SubscribeEvent
    public void onLevelSave(LevelEvent.Save event) {
        TabletCache.server().saveAll((Level) event.getLevel());
    }

    @SubscribeEvent
    public void onPlayerSave(PlayerEvent.SaveToFile event) {
        TabletCache.server().save(event.getEntity());
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        Level level = (Level) event.getLevel();
        TabletCache.client().clear(level);
        TabletCache.server().clear(level);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event) {
        TabletCache.server().cleanUp();
    }

    /** Advance a running tablet held by a player's client-side cursor. */
    public static void tickCursorHeld(ServerPlayer player, String id) {
        long gameTime = player.level().getGameTime();
        UUID key = player.getUUID();
        CursorTick previous = LAST_CURSOR_TICKS.get(key);
        if (previous != null && previous.id.equals(id) && previous.gameTime == gameTime) return;

        Option<TabletWrapper> cached = TabletCache.server().get(id);
        if (cached.isDefined() && cached.get().player == player) {
            LAST_CURSOR_TICKS.put(key, new CursorTick(id, gameTime));
            cached.get().update(player.level(), player, -1, false);
        }
    }

    private record CursorTick(String id, long gameTime) { }
}

