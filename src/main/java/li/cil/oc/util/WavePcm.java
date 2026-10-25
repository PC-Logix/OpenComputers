package li.cil.oc.util;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/** Pure PCM synthesis, independent of Minecraft and the native sound engine. */
final class WavePcm {
    private WavePcm() { }
    static byte[] generate(int sampleRate, int amplitude, int mode, int frequency, int durationMillis, int delayMillis,
                           int fmFrequency, float fmIntensity, int amFrequency, int attack, int decay, float sustain, int release) {
        int delay = Math.clamp(delayMillis, 0, 16000), duration = Math.clamp(durationMillis, 50, 5000);
        byte[] data = new byte[(delay + duration) * sampleRate / 1000]; Arrays.fill(data, (byte) 127);
        int start = delay * sampleRate / 1000;
        double phase = 0, fmPhase = 0, amPhase = 0;
        for (int i = 0; i < duration * sampleRate / 1000 && start + i < data.length; i++) {
            double elapsed = i * 1000.0 / sampleRate;
            double fm = fmFrequency > 0 ? Math.sin(fmPhase * 2 * Math.PI) * fmIntensity * fmFrequency : 0;
            double value = switch (mode) {
                case 1 -> Math.sin(phase * 2 * Math.PI);
                case 2 -> 1 - 4 * Math.abs(phase - 0.5);
                case 3 -> 2 * phase - 1;
                case 4 -> ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
                default -> phase < 0.5 ? 1 : -1;
            };
            double attackGain = attack > 0 && elapsed < attack ? elapsed / attack : 1;
            double decayGain = decay > 0 && elapsed >= attack && elapsed < attack + decay ? 1 - (1 - sustain) * (elapsed - attack) / decay : sustain;
            double releaseGain = release > 0 && elapsed > duration - release ? Math.max(0, (duration - elapsed) / release) : 1;
            double amGain = amFrequency > 0 ? 0.5 + 0.5 * Math.sin(amPhase * 2 * Math.PI) : 1;
            data[start + i] = (byte) (127 + value * amplitude * attackGain * decayGain * releaseGain * amGain);
            phase = (phase + Math.max(20, Math.min(2000, frequency + fm)) / sampleRate) % 1;
            // Preserve the original Scala integer arithmetic for modulation phases.
            fmPhase = (fmPhase + Math.max(20, fmFrequency) / sampleRate) % 1;
            amPhase = (amPhase + Math.max(20, amFrequency) / sampleRate) % 1;
        }
        return data;
    }
}
