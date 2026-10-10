package li.cil.oc.server.component;

import org.lwjgl.openal.AL10;

import java.io.ByteArrayOutputStream;

final class AudioCardSession {
    public final int handle;
    public final int channel;
    public final int sampleRate;
    public final String mode;
    public final int format;
    public final int channels;
    public final int bytesPerSample;

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    public boolean loop;
    public boolean closed;
    private boolean playing;
    private boolean paused;
    private long playStartTime;
    private long remainingDurationMs;

    AudioCardSession(int handle, int channel, int sampleRate, String mode) {
        this.handle = handle;
        this.channel = channel;
        this.sampleRate = sampleRate;
        this.mode = mode;
        switch (mode) {
            case "mono8" -> {
                format = AL10.AL_FORMAT_MONO8;
                channels = 1;
                bytesPerSample = 1;
            }
            case "mono16" -> {
                format = AL10.AL_FORMAT_MONO16;
                channels = 1;
                bytesPerSample = 2;
            }
            case "stereo8" -> {
                format = AL10.AL_FORMAT_STEREO8;
                channels = 2;
                bytesPerSample = 1;
            }
            case "stereo16" -> {
                format = AL10.AL_FORMAT_STEREO16;
                channels = 2;
                bytesPerSample = 2;
            }
            default -> throw new IllegalArgumentException("unknown audio mode: '" + mode
                + "' (valid: mono8, mono16, stereo8, stereo16)");
        }
    }

    public int size() { return buffer.size(); }
    public void append(byte[] data) { buffer.write(data, 0, data.length); }
    public byte[] pcm() { return buffer.toByteArray(); }

    public void startPlayback() {
        playing = true;
        paused = false;
        remainingDurationMs = size() * 1000L / (sampleRate * channels * bytesPerSample);
        playStartTime = System.currentTimeMillis();
    }

    public void pausePlayback() {
        if (playing && !paused) {
            paused = true;
            long elapsed = System.currentTimeMillis() - playStartTime;
            remainingDurationMs = Math.max(0L, remainingDurationMs - elapsed);
        }
    }

    public void resumePlayback() {
        if (playing && paused) {
            paused = false;
            playStartTime = System.currentTimeMillis();
        }
    }

    public void stopPlayback() {
        playing = false;
        paused = false;
        remainingDurationMs = 0L;
    }

    public boolean isPlayingNow() {
        if (!playing) return false;
        if (loop || paused) return true;
        long elapsed = System.currentTimeMillis() - playStartTime;
        if (elapsed >= remainingDurationMs) {
            playing = false;
            return false;
        }
        return true;
    }
}
