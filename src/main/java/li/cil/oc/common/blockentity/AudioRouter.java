package li.cil.oc.common.blockentity;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.blockentity.traits.NetworkBlockEntity;
import li.cil.oc.integration.multipart.ModCBMultipart;
import li.cil.oc.server.component.AudioCard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.*;

public final class AudioRouter {
    public record Session(EnvironmentHost host, int handle) { }
    private AudioRouter() { }
    private static List<EnvironmentHost> outputs(NetworkBlockEntity source) {
        var level = source.getLevel();
        if (level == null || level.isClientSide()) return List.of();
        var pending = new ArrayDeque<BlockPos>(); var visited = new HashSet<BlockPos>(); var speakers = new ArrayList<EnvironmentHost>();
        pending.add(source.getBlockPos()); visited.add(source.getBlockPos());
        while (!pending.isEmpty() && visited.size() <= 64) {
            BlockPos current = pending.remove();
            for (Direction direction : Direction.values()) {
                BlockPos next = li.cil.oc.util.BlockPosHelper.relative(current, direction);
                boolean canPass = ModCBMultipart.canAudioConnectFromSide(level, current, direction) && ModCBMultipart.canAudioConnectFromSide(level, next, direction.getOpposite());
                if (!visited.contains(next) && canPass) {
                    var entity = level.getBlockEntity(next);
                    if (entity instanceof AudioCable || ModCBMultipart.isAudioCable(level, next)) { visited.add(next); pending.add(next); }
                    else if (entity instanceof Speaker speaker) { visited.add(next); speakers.add(speaker); }
                }
            }
        }
        return speakers.isEmpty() ? List.of(source) : speakers;
    }
    public static List<Session> play(NetworkBlockEntity source, byte[] pcm) { return play(source, pcm, 32768); }
    public static List<Session> play(NetworkBlockEntity source, byte[] pcm, int rate) {
        List<Session> result = new ArrayList<>();
        for (var output : outputs(source)) { var handle = AudioCard.playPcm(output, pcm, rate, 1024); if (handle.isDefined()) result.add(new Session(output, handle.get())); }
        return result;
    }
    public static List<Session> playDfpwm(NetworkBlockEntity source, byte[] encoded, int rate, float volume) {
        List<Session> result = new ArrayList<>();
        for (var output : outputs(source)) { var handle = AudioCard.playDfpwm(output, encoded, rate, volume); if (handle.isDefined()) result.add(new Session(output, handle.get())); }
        return result;
    }
    public static void stop(List<Session> sessions) { for (var session : sessions) li.cil.oc.server.PacketSender.sendAudioStop(session.host(), session.handle()); }
}
