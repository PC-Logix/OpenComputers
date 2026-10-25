package li.cil.oc.server;

import li.cil.oc.Settings;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.*;
import li.cil.oc.util.SableCompat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.io.IOException;

/** Audio packets use physical positions so listeners can hear moving sublevel sources. */
public final class PacketSender {
    private PacketSender() { }
    private static void listeners(Level level, Vec3 source, PacketBuilder packet) {
        if (level == null) return;
        Vec3 physical = SableCompat.physicalPosition(level, source);
        double range = Settings.get().maxNetworkClientSoundPacketDistance();
        for (var player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player.level().dimension().equals(level.dimension()) &&
                SableCompat.physicalPosition(player.level(), player.position()).distanceToSqr(physical) <= range * range) packet.sendToPlayer(player);
        }
    }
    private static void listeners(EnvironmentHost host, PacketBuilder packet) { listeners(host.getEnvironmentLevel(), new Vec3(host.xPosition(), host.yPosition(), host.zPosition()), packet); }
    public static void sendComputronicsTone(Level level, double x, double y, double z, int mode, int frequency, int duration, int delay, double volume) {
        sendComputronicsTone(level, x, y, z, mode, frequency, duration, delay, volume, 0, 0, 0, 0, 0, 1, 0);
    }
    public static void sendComputronicsTone(Level level, double x, double y, double z, int mode, int frequency, int duration, int delay, double volume,
                                            int fmFrequency, double fmIntensity, int amFrequency, int attack, int decay, double sustain, int release) {
        write(() -> {
            var packet = new SimplePacketBuilder(PacketType.ComputronicsTone);
            Vec3 pos = SableCompat.physicalPosition(level, new Vec3(x, y, z));
            packet.writeDouble(pos.x); packet.writeDouble(pos.y); packet.writeDouble(pos.z);
            packet.writeByte(mode); packet.writeShort((short) frequency); packet.writeShort((short) duration); packet.writeShort((short) delay);
            packet.writeFloat((float) volume); packet.writeShort((short) fmFrequency); packet.writeFloat((float) fmIntensity);
            packet.writeShort((short) amFrequency); packet.writeShort((short) attack); packet.writeShort((short) decay);
            packet.writeFloat((float) sustain); packet.writeShort((short) release);
            listeners(level, new Vec3(x, y, z), packet);
        });
    }
    public static void sendAudioStart(EnvironmentHost host, int id, int channel, int rate, int channels, int format, boolean loop) {
        write(() -> {
            var packet = new SimplePacketBuilder(PacketType.AudioStart);
            packet.writeInt(id); packet.writeInt(channel); packet.writeInt(rate); packet.writeInt(channels); packet.writeInt(format); packet.writeBoolean(loop);
            position(host, packet); listeners(host, packet);
        });
    }
    public static void sendTapeAudioStart(EnvironmentHost host, int id, int rate, float volume) {
        write(() -> { var packet = new SimplePacketBuilder(PacketType.TapeAudioStart); packet.writeInt(id); packet.writeInt(rate); packet.writeFloat(volume); position(host, packet); listeners(host, packet); });
    }
    private static void position(EnvironmentHost host, SimplePacketBuilder packet) throws IOException {
        Vec3 pos = SableCompat.physicalPosition(host); packet.writeDouble(pos.x); packet.writeDouble(pos.y); packet.writeDouble(pos.z);
    }
    public static void sendAudioChunk(EnvironmentHost host, int id, byte[] data) {
        write(() -> { var packet = new CompressedPacketBuilder(PacketType.AudioChunk); packet.writeInt(id); packet.writeInt(data.length); packet.write(data); listeners(host, packet); });
    }
    public static void sendAudioPlay(EnvironmentHost host, int id) { session(host, id, PacketType.AudioPlay); }
    public static void sendAudioPause(EnvironmentHost host, int id) { session(host, id, PacketType.AudioPause); }
    public static void sendAudioResume(EnvironmentHost host, int id) { session(host, id, PacketType.AudioResume); }
    public static void sendAudioStop(EnvironmentHost host, int id) { session(host, id, PacketType.AudioStop); }
    public static void sendAudioClose(EnvironmentHost host, int id) { session(host, id, PacketType.AudioClose); }
    private static void session(EnvironmentHost host, int id, PacketType type) { write(() -> { var packet = new SimplePacketBuilder(type); packet.writeInt(id); listeners(host, packet); }); }
    public static void sendAudioSetLoop(EnvironmentHost host, int id, boolean loop) {
        write(() -> { var packet = new SimplePacketBuilder(PacketType.AudioSetLoop); packet.writeInt(id); packet.writeBoolean(loop); listeners(host, packet); });
    }
    @FunctionalInterface private interface Write { void run() throws IOException; }
    private static void write(Write action) { try { action.run(); } catch (IOException error) { PacketSender.<RuntimeException>raise(error); } }
    @SuppressWarnings("unchecked") private static <E extends Throwable> void raise(Throwable error) throws E { throw (E) error; }
}
