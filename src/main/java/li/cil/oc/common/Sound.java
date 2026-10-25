package li.cil.oc.common;

import li.cil.oc.Settings;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.server.SoundEffectPacketSender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class Sound {
    private static final Map<EnvironmentHost, Map<String, Long>> GLOBAL_TIMEOUTS = new WeakHashMap<>();

    private Sound() {}

    public static synchronized void play(EnvironmentHost host, String name) {
        long now = System.currentTimeMillis();
        Map<String, Long> hostTimeouts = GLOBAL_TIMEOUTS.get(host);
        if (hostTimeouts != null && hostTimeouts.getOrDefault(name, 0L) > now) return;

        SoundEffectPacketSender.sendEffectUnchecked(
                host.getEnvironmentLevel(), host.xPosition(), host.yPosition(), host.zPosition(),
                ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), name),
                SoundSource.BLOCKS, 15 * Settings.get().soundVolume());
        GLOBAL_TIMEOUTS.computeIfAbsent(host, ignored -> new HashMap<>()).put(name, now + 500);
    }

    public static void playDiskInsert(EnvironmentHost host) {
        play(host, "floppy_insert");
    }

    public static void playDiskEject(EnvironmentHost host) {
        play(host, "floppy_eject");
    }
}
