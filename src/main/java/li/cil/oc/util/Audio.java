package li.cil.oc.util;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.client.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

/** Computer speaker PCM generation and sound-thread-owned OpenAL resources. */
public final class Audio {
    public static final Audio INSTANCE = new Audio();
    private static final Set<Source> sources = new HashSet<>();
    private static boolean disableAudio;
    private Audio() { }
    private static int sampleRate() { return Settings.get().beepSampleRate(); }
    private static int amplitude() { return Settings.get().beepAmplitude(); }
    private static float maxDistance() { return Settings.get().beepRadius(); }
    private static float volume() { return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.BLOCKS); }
    public static Vec3 localAudioPosition(Vec3 physical) {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return physical;
        var listener = mc.player.position(); return listener.add(physical.subtract(SableCompat.physicalPosition(mc.level, listener)));
    }
    public static double physicalGain(Vec3 physical, double distance) {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return 0;
        return Math.max(0, 1 - SableCompat.physicalPosition(mc.level, mc.player.position()).distanceTo(physical) / distance);
    }
    /** The executor can exist before OpenAL is ready, or outlive it during reload. */
    public static boolean runOnSoundEngine(Runnable action) {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.getSoundManager() == null || mc.getSoundManager().soundEngine == null) return false;
        var engine = mc.getSoundManager().soundEngine;
        if (!engine.loaded || engine.executor == null) return false;
        ((Executor) engine.executor).execute(() -> { if (engine.loaded) action.run(); }); return true;
    }
    public static void play(float x, float y, float z, byte[] pcm, float gain) {
        if (pcm == null || pcm.length == 0) return;
        runOnSoundEngine(() -> {
            try {
                // OpenAL requires a native address; a heap ByteBuffer can crash on Windows.
                var nativePcm = BufferUtils.createByteBuffer(pcm.length); nativePcm.put(pcm).flip();
                synchronized (sources) { sources.add(new Source(x, y, z, nativePcm, gain)); }
            } catch (OpenALException error) { if (error.errorCode() == AL10.AL_OUT_OF_MEMORY) disableAudio = true; }
        });
    }
    public static void play(float x, float y, float z, int frequency, int duration) { play(x, y, z, ".", frequency, duration); }
    public static void playWave(float x, float y, float z, int mode, int frequency, int duration, int delay, float requestedGain,
                                int fmFrequency, float fmIntensity, int amFrequency, int attack, int decay, float sustain, int release) {
        var mc = Minecraft.getInstance(); if (mc == null || mc.player == null) return;
        var physical = new Vec3(x, y, z);
        float gain = (float) physicalGain(physical, maxDistance()) * volume() * Math.clamp(requestedGain, 0, 1);
        if (gain <= 0 || amplitude() <= 0) return;
        byte[] data = WavePcm.generate(sampleRate(), amplitude(), mode, frequency, duration, delay, fmFrequency, fmIntensity, amFrequency, attack, decay, sustain, release);
        var local = localAudioPosition(physical); play((float) local.x, (float) local.y, (float) local.z, data, gain);
    }
    public static void play(float x, float y, float z, String pattern) { play(x, y, z, pattern, 1000, 200); }
    public static void play(float x, float y, float z, String pattern, int frequency, int duration) {
        var mc = Minecraft.getInstance();
        float gain = (float) Math.max(0, 1 - mc.player.position().distanceTo(new Vec3(x, y, z)) / maxDistance()) * volume();
        if (gain <= 0 || amplitude() <= 0) return;
        if (disableAudio) {
            float pitch = Math.clamp(frequency - 20, 0, 1980) / 1980f + 0.5f;
            int delay = 0;
            for (char ch : pattern.toCharArray()) {
                var sound = new SimpleSoundInstance(SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, gain, pitch, mc.level.random, new BlockPos((int) x, (int) y, (int) z));
                if (delay == 0) mc.getSoundManager().play(sound); else mc.getSoundManager().playDelayed(sound, delay);
                delay += Math.max(1, (ch == '.' ? duration : 2 * duration) * 20 / 1000);
            }
            return;
        }
        int[] counts = pattern.chars().map(ch -> (ch == '.' ? duration : 2 * duration) * sampleRate() / 1000).toArray();
        int pause = 50 * sampleRate() / 1000, total = java.util.Arrays.stream(counts).sum() + (counts.length - 1) * pause;
        var data = BufferUtils.createByteBuffer(total); float step = frequency / (float) sampleRate(), offset = 0;
        for (int count : counts) {
            for (int i = 0; i < count; i++) {
                byte value = (byte) (Math.signum(Math.sin(2 * Math.PI * offset)) * amplitude());
                offset += step; if (offset > 1) offset -= 1; data.put((byte) (value ^ 0x80));
            }
            if (data.hasRemaining()) for (int i = 0; i < pause; i++) data.put((byte) 127);
        }
        data.rewind();
        runOnSoundEngine(() -> {
            try { synchronized (sources) { sources.add(new Source(x, y, z, data, gain)); } }
            catch (OpenALException error) {
                if (error.errorCode() == AL10.AL_OUT_OF_MEMORY) {
                    OpenComputers.log.info("Couldn't play computer speaker sound because your sound card ran out of memory. Disabling computer speakers to avoid spamming your log file now.");
                    disableAudio = true;
                } else OpenComputers.log.warn("Error playing computer speaker sound.", error);
            }
        });
    }
    public static void update() {
        boolean hasSources; synchronized (sources) { hasSources = !sources.isEmpty(); }
        if (!disableAudio && hasSources) runOnSoundEngine(() -> {
            synchronized (sources) { sources.removeIf(Source::checkFinished); }
            try { AL10.alGetError(); }
            catch (UnsatisfiedLinkError error) { OpenComputers.log.warn("Negotiations with OpenAL broke down, disabling sounds."); disableAudio = true; }
        });
        PacketHandler.update();
    }
    private static final class Source {
        private final int source, buffer;
        Source(float x, float y, float z, ByteBuffer data, float gain) {
            AL10.alGetError(); buffer = AL10.alGenBuffers(); checkALError();
            try {
                AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO8, data, sampleRate()); checkALError();
                source = AL10.alGenSources(); checkALError();
                try {
                    AL10.alSourceQueueBuffers(source, buffer); checkALError();
                    AL10.alSource3f(source, AL10.AL_POSITION, x, y, z);
                    AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, maxDistance());
                    AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, maxDistance());
                    AL10.alSourcef(source, AL10.AL_GAIN, gain * 0.3f); checkALError();
                    AL10.alSourcePlay(source); checkALError();
                } catch (Throwable error) { AL10.alDeleteSources(source); throw error; }
            } catch (Throwable error) { AL10.alDeleteBuffers(buffer); throw error; }
        }
        boolean checkFinished() {
            if (AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) return false;
            AL10.alDeleteSources(source); AL10.alDeleteBuffers(buffer); return true;
        }
    }
    public static final class OpenALException extends RuntimeException {
        private final int errorCode;
        public OpenALException(int errorCode) { super(Integer.toString(errorCode)); this.errorCode = errorCode; }
        public int errorCode() { return errorCode; }
    }
    public static void checkALError() {
        int code = AL10.alGetError(); if (code != AL10.AL_NO_ERROR) throw new OpenALException(code);
    }
    @SubscribeEvent public void onTick(ClientTickEvent.Pre event) { update(); }
}
