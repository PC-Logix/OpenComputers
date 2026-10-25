package li.cil.oc.client.audio;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.util.Audio;
import li.cil.oc.util.DFPWM;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import java.security.MessageDigest;
import java.util.*;

/** A sound-thread-owned OpenAL stream with one persistent DFPWM decoder. */
public final class AudioSession {
    private static final int BUFFER_SIZE = 8192, BUFFER_COUNT = 3;
    private final int handle, channel, sampleRate, channels, format;
    private final Vec3 pos;
    private final boolean encodedDfpwm;
    private final float streamGain;
    private final Queue<byte[]> pendingBuffers = new ArrayDeque<>();
    private final List<Integer> nativeBuffers = new ArrayList<>();
    private final DFPWM decoder;
    private final MessageDigest digest;
    private long appendedBytes;
    private volatile boolean loop, playCalled, finished;
    private volatile int alSource = -1, alBuffer = -1;

    public AudioSession(int handle, int channel, int sampleRate, int channels, int format, Vec3 pos, boolean encodedDfpwm, float streamGain) {
        this.handle = handle; this.channel = channel; this.sampleRate = sampleRate; this.channels = channels;
        this.format = format; this.pos = pos; this.encodedDfpwm = encodedDfpwm; this.streamGain = streamGain;
        decoder = encodedDfpwm ? new DFPWM() : null;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    public int handle() { return handle; }
    public int channel() { return channel; }
    public int sampleRate() { return sampleRate; }
    public int channels() { return channels; }
    public int format() { return format; }
    public Vec3 pos() { return pos; }
    public boolean encodedDfpwm() { return encodedDfpwm; }
    public float streamGain() { return streamGain; }
    public boolean loop() { return loop; }
    public void append(byte[] data) {
        if (playCalled) return;
        byte[] pcm = data;
        if (encodedDfpwm) {
            pcm = new byte[data.length * 8];
            decoder.decompress(pcm, data, 0, 0, data.length);
            for (int i = 0; i < pcm.length; i++) pcm[i] = (byte) (pcm[i] ^ 0x80);
        }
        appendedBytes += pcm.length; digest.update(pcm);
        // Preserve Computronics' 1024 encoded bytes -> 8192 MONO8 samples cadence.
        for (int offset = 0; offset < pcm.length; offset += BUFFER_SIZE)
            pendingBuffers.add(Arrays.copyOfRange(pcm, offset, Math.min(pcm.length, offset + BUFFER_SIZE)));
    }
    public void play() {
        if (playCalled) {
            if (alSource != -1) Audio.runOnSoundEngine(() -> { if (alSource != -1) { finished = false; AL10.alSourcePlay(alSource); } });
            return;
        }
        playCalled = true;
        OpenComputers.log.info("Audio stream play: handle={}, sampleRate={}, format={}, pcmBytes={}, pcmSha256={}, buffers={}",
            handle, sampleRate, format, appendedBytes, HexFormat.of().formatHex(digest.digest()), pendingBuffers.size());
        if (pendingBuffers.isEmpty()) return;
        if (!Audio.runOnSoundEngine(() -> {
            try {
                AL10.alGetError(); alSource = AL10.alGenSources(); Audio.checkALError();
                if (loop) {
                    int length = pendingBuffers.stream().mapToInt(bytes -> bytes.length).sum();
                    byte[] pcm = new byte[length]; int offset = 0;
                    while (!pendingBuffers.isEmpty()) { byte[] part = pendingBuffers.remove(); System.arraycopy(part, 0, pcm, offset, part.length); offset += part.length; }
                    alBuffer = createBuffer(pcm);
                } else {
                    for (int i = 0; i < BUFFER_COUNT && !pendingBuffers.isEmpty(); i++) createBuffer(pendingBuffers.remove());
                }
                var local = Audio.localAudioPosition(pos);
                AL10.alSource3f(alSource, AL10.AL_POSITION, (float) local.x, (float) local.y, (float) local.z);
                float radius = Settings.get().beepRadius();
                AL10.alSourcef(alSource, AL10.AL_REFERENCE_DISTANCE, radius);
                AL10.alSourcef(alSource, AL10.AL_MAX_DISTANCE, radius);
                updateSpatialAudio();
                AL10.alSourcei(alSource, AL10.AL_LOOPING, loop ? AL10.AL_TRUE : AL10.AL_FALSE);
                Audio.checkALError(); AL10.alSourcePlay(alSource); Audio.checkALError();
            } catch (Throwable error) { OpenComputers.log.error("Failed to play audio", error); cleanupOnSoundThread(); }
        })) playCalled = false;
    }
    private int createBuffer(byte[] pcm) {
        int buffer = AL10.alGenBuffers(); Audio.checkALError(); nativeBuffers.add(buffer);
        fillBuffer(buffer, pcm); AL10.alSourceQueueBuffers(alSource, buffer); Audio.checkALError(); return buffer;
    }
    private void fillBuffer(int buffer, byte[] pcm) {
        var nativeData = BufferUtils.createByteBuffer(pcm.length); nativeData.put(pcm).flip();
        AL10.alBufferData(buffer, format, nativeData, sampleRate); Audio.checkALError();
    }
    public void pause() {
        if (alSource != -1) Audio.runOnSoundEngine(() -> {
            if (alSource != -1 && AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) AL10.alSourcePause(alSource);
        });
    }
    public void resume() {
        if (alSource != -1) Audio.runOnSoundEngine(() -> {
            if (alSource != -1 && AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE) == AL10.AL_PAUSED) { finished = false; AL10.alSourcePlay(alSource); }
        });
    }
    public void stop() { if (alSource != -1) Audio.runOnSoundEngine(() -> { if (alSource != -1) AL10.alSourceStop(alSource); }); }
    public void setLoopMode(boolean loop) {
        this.loop = loop;
        if (alSource != -1) Audio.runOnSoundEngine(() -> { if (alSource != -1) AL10.alSourcei(alSource, AL10.AL_LOOPING, this.loop ? AL10.AL_TRUE : AL10.AL_FALSE); });
    }
    public void update() {
        if (playCalled && alSource != -1) Audio.runOnSoundEngine(() -> {
            if (alSource == -1) return;
            updateSpatialAudio(); if (!loop) refillProcessedBuffers();
            int state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE), queued = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_QUEUED);
            finished = !loop && pendingBuffers.isEmpty() && queued == 0 && state != AL10.AL_PLAYING && state != AL10.AL_PAUSED;
        });
    }
    public boolean checkFinished() { return finished; }
    public void cleanup() {
        if (!Audio.runOnSoundEngine(this::cleanupOnSoundThread)) { alSource = -1; alBuffer = -1; finished = true; }
    }
    private void cleanupOnSoundThread() {
        if (alSource != -1) { try { AL10.alDeleteSources(alSource); } catch (Throwable ignored) { } alSource = -1; }
        // Delete the source first to release queued buffers.
        for (int buffer : nativeBuffers) { try { AL10.alDeleteBuffers(buffer); } catch (Throwable ignored) { } }
        nativeBuffers.clear(); pendingBuffers.clear(); alBuffer = -1; finished = true;
    }
    private void refillProcessedBuffers() {
        int processed = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            int buffer = AL10.alSourceUnqueueBuffers(alSource);
            if (!pendingBuffers.isEmpty()) { fillBuffer(buffer, pendingBuffers.remove()); AL10.alSourceQueueBuffers(alSource, buffer); Audio.checkALError(); }
        }
        int state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE);
        if (state != AL10.AL_PLAYING && !pendingBuffers.isEmpty() && AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_QUEUED) > 0) AL10.alSourcePlay(alSource);
    }
    private void updateSpatialAudio() {
        var mc = Minecraft.getInstance(); if (mc == null || mc.player == null || mc.level == null) return;
        var local = Audio.localAudioPosition(pos);
        AL10.alSource3f(alSource, AL10.AL_POSITION, (float) local.x, (float) local.y, (float) local.z);
        float gain = (float) Audio.physicalGain(pos, Settings.get().beepRadius()) * mc.options.getSoundSourceVolume(SoundSource.BLOCKS);
        // Tape volume matches Computronics; the generic PCM card retains its 0.3 multiplier.
        AL10.alSourcef(alSource, AL10.AL_GAIN, gain * streamGain * (encodedDfpwm ? 1 : 0.3f));
    }
}
