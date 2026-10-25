package li.cil.oc.client;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.lang.ref.WeakReference;
import java.util.*;

/** World-positioned computer loops, scheduled on the client thread. */
public final class Sound {
    public static final Sound INSTANCE = new Sound();
    private static final Map<BlockEntity, PseudoLoopingStream> sources = new WeakHashMap<>();
    // Keep engine-owned sounds reachable even after their weak entity key disappears.
    private static final Set<PseudoLoopingStream> activeSounds = new HashSet<>();
    private static final PriorityQueue<Command> commands = new PriorityQueue<>(Comparator.comparingLong(command -> command.when));
    private static final Timer timer = new Timer("OpenComputers-SoundUpdater", true);
    private static boolean updatePending;
    static {
        if (Settings.get().soundVolume() > 0) timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { synchronized (sources) { updatePending = true; } }
        }, 500, 50);
    }
    private Sound() { }
    private static void processQueue() {
        synchronized (commands) {
            while (!commands.isEmpty() && commands.peek().when < System.currentTimeMillis()) {
                try { commands.remove().run(); }
                catch (Throwable error) { OpenComputers.log.warn("Error processing sound command.", error); }
            }
        }
    }
    public static void startLoop(BlockEntity entity, String name) { startLoop(entity, name, 1, 0); }
    public static void startLoop(BlockEntity entity, String name, float volume, long delay) {
        if (Settings.get().soundVolume() > 0) synchronized (commands) { commands.add(new StartCommand(entity, name, volume, delay)); }
    }
    public static void stopLoop(BlockEntity entity) {
        if (Settings.get().soundVolume() > 0) synchronized (commands) { commands.add(new StopCommand(entity)); }
    }
    public static void updatePosition(BlockEntity entity) {
        if (Settings.get().soundVolume() > 0) synchronized (commands) { commands.add(new UpdateCommand(entity)); }
    }
    @SubscribeEvent public void onTick(ClientTickEvent.Pre event) {
        synchronized (sources) { if (updatePending) processQueue(); updatePending = false; }
    }
    @SubscribeEvent public void onWorldUnload(LevelEvent.Unload event) { stopAll(); }
    public static void stopAll() {
        synchronized (commands) { commands.clear(); }
        synchronized (sources) {
            try { for (var sound : List.copyOf(activeSounds)) stopSound(sound); }
            catch (Throwable ignored) { }
            activeSounds.clear(); sources.clear();
        }
    }
    private static void stopSound(PseudoLoopingStream sound) {
        sound.stop();
        var minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.getSoundManager() != null) minecraft.getSoundManager().stop(sound);
        activeSounds.remove(sound);
    }
    private abstract static class Command {
        final long when;
        final WeakReference<BlockEntity> entity;
        Command(BlockEntity entity, long when) { this.entity = new WeakReference<>(entity); this.when = when; }
        abstract void run();
    }
    private static final class StartCommand extends Command {
        private final String name;
        private final float volume;
        StartCommand(BlockEntity entity, String name, float volume, long delay) {
            super(entity, System.currentTimeMillis() + delay); this.name = name; this.volume = volume;
        }
        @Override void run() {
            var blockEntity = entity.get(); if (blockEntity == null) return;
            synchronized (sources) {
                var current = sources.get(blockEntity);
                if (current == null || !current.getLocation().getPath().equals(name)) {
                    if (current != null) stopSound(current);
                    var sound = new PseudoLoopingStream(entity, volume, name);
                    sources.put(blockEntity, sound); activeSounds.add(sound);
                    Minecraft.getInstance().getSoundManager().play(sound);
                }
            }
        }
    }
    private static final class StopCommand extends Command {
        StopCommand(BlockEntity entity) { super(entity, System.currentTimeMillis() + 1); }
        @Override void run() {
            var blockEntity = entity.get(); if (blockEntity == null) return;
            synchronized (sources) { var sound = sources.remove(blockEntity); if (sound != null) stopSound(sound); }
            synchronized (commands) { commands.removeIf(command -> command.entity.get() == null || command.entity.get() == blockEntity); }
        }
    }
    private static final class UpdateCommand extends Command {
        UpdateCommand(BlockEntity entity) { super(entity, System.currentTimeMillis()); }
        @Override void run() {
            var blockEntity = entity.get(); if (blockEntity == null) return;
            synchronized (sources) { var sound = sources.get(blockEntity); if (sound != null) sound.updatePosition(); }
        }
    }
    private static final class PseudoLoopingStream extends AbstractSoundInstance implements TickableSoundInstance {
        private final WeakReference<BlockEntity> entity;
        private boolean stopped;
        PseudoLoopingStream(WeakReference<BlockEntity> entity, float subVolume, String name) {
            super(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, name), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.entity = entity; volume = subVolume * Settings.get().soundVolume();
            relative = false; attenuation = Attenuation.LINEAR; looping = true; updatePosition();
        }
        void updatePosition() {
            var blockEntity = entity.get(); if (blockEntity == null) return;
            var pos = blockEntity.getBlockPos(); x = pos.getX() + 0.5; y = pos.getY() + 0.5; z = pos.getZ() + 0.5;
        }
        @Override public boolean canStartSilent() { return true; }
        @Override public boolean isStopped() { return stopped; }
        @Override public void tick() { if (entity.get() != null) updatePosition(); else stopSound(this); }
        @Override public float getVolume() {
            var listener = Minecraft.getInstance().player; if (listener == null) return 0;
            double distance = Math.sqrt(listener.distanceToSqr(x, y, z));
            return distance >= 16 ? 0 : super.getVolume() * (float) (1 - distance / 16);
        }
        void stop() { stopped = true; looping = false; }
    }
}
