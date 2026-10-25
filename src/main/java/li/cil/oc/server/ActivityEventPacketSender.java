package li.cil.oc.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.event.NetworkActivityEvent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.PacketBuilder;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class ActivityEventPacketSender {
    private static final Map<Node, Cache<String, Long>> FILE_SYSTEM_ACCESS_TIMEOUTS = new WeakHashMap<>();

    private ActivityEventPacketSender() {}

    public static void sendFileSystemActivity(Node node, EnvironmentHost host, String name,
                                             int delay, int threads, double range) throws IOException {
        if (delay < 0) return;
        Cache<String, Long> hostTimeouts = timeouts(node, delay, threads);
        Long lastTimeout = hostTimeouts.getIfPresent(name);
        if (lastTimeout != null && lastTimeout > System.currentTimeMillis()) return;

        FileSystemAccessEvent.Server event = host instanceof BlockEntity tile
                ? new FileSystemAccessEvent.Server(name, tile, node)
                : new FileSystemAccessEvent.Server(name, host.getEnvironmentLevel(), host.xPosition(), host.yPosition(), host.zPosition(), node);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return;
        hostTimeouts.put(name, System.currentTimeMillis() + delay);
        sendFileSystemActivity(event, host, range);
    }

    public static void sendFileSystemActivity(Node node, EnvironmentHost host,
                                             int delay, int threads, double range) throws IOException {
        if (delay < 0) return;
        Cache<String, Long> hostTimeouts = timeouts(node, delay, threads);
        String cacheKey = host instanceof BlockEntity tile
                ? tile.getBlockPos().toString()
                : host.xPosition() + "," + host.yPosition() + "," + host.zPosition();
        Long lastTimeout = hostTimeouts.getIfPresent(cacheKey);
        if (lastTimeout != null && lastTimeout > System.currentTimeMillis()) return;

        FileSystemAccessEvent.Server event = host instanceof BlockEntity tile
                ? new FileSystemAccessEvent.Server(null, tile, node)
                : new FileSystemAccessEvent.Server(null, host.getEnvironmentLevel(), host.xPosition(), host.yPosition(), host.zPosition(), node);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return;
        hostTimeouts.put(cacheKey, System.currentTimeMillis() + delay);
        sendFileSystemActivity(event, host, range);
    }

    private static Cache<String, Long> timeouts(Node node, int delay, int threads) {
        synchronized (FILE_SYSTEM_ACCESS_TIMEOUTS) {
            return FILE_SYSTEM_ACCESS_TIMEOUTS.computeIfAbsent(node, ignored -> CacheBuilder.newBuilder()
                    .concurrencyLevel(threads)
                    .maximumSize(250)
                    .expireAfterWrite(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .build());
        }
    }

    private static void sendFileSystemActivity(FileSystemAccessEvent.Server event, EnvironmentHost host, double range) throws IOException {
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.FileSystemActivity);
        packet.writeUTF(event.getSound());
        NbtIo.write(event.getData(), packet);
        writeEventPosition(packet, event);
        packet.sendToPlayersNearHost(host, Optional.of(range));
    }

    public static void sendNetworkActivity(Node node, EnvironmentHost host, double range) throws IOException {
        NetworkActivityEvent.Server event = host instanceof BlockEntity tile
                ? new NetworkActivityEvent.Server(tile, node)
                : new NetworkActivityEvent.Server(host.getEnvironmentLevel(), host.xPosition(), host.yPosition(), host.zPosition(), node);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return;

        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.NetworkActivity);
        NbtIo.write(event.getData(), packet);
        writeEventPosition(packet, event);
        packet.sendToPlayersNearHost(host, Optional.of(range));
    }

    public static void sendFileSystemActivityUnchecked(Node node, EnvironmentHost host, String name,
                                                       int delay, int threads, double range) {
        try {
            sendFileSystemActivity(node, host, name, delay, threads, range);
        } catch (IOException error) {
            ActivityEventPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendFileSystemActivityUnchecked(Node node, EnvironmentHost host,
                                                       int delay, int threads, double range) {
        try {
            sendFileSystemActivity(node, host, delay, threads, range);
        } catch (IOException error) {
            ActivityEventPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    public static void sendNetworkActivityUnchecked(Node node, EnvironmentHost host, double range) {
        try {
            sendNetworkActivity(node, host, range);
        } catch (IOException error) {
            ActivityEventPacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }

    private static void writeEventPosition(PacketBuilder packet, FileSystemAccessEvent event) throws IOException {
        if (event.getBlockEntity() != null) {
            packet.writeBoolean(true);
            packet.writeTileEntity(event.getBlockEntity());
        } else {
            packet.writeBoolean(false);
            packet.writeUTF(event.getWorld().dimension().location().toString());
            packet.writeDouble(event.getX());
            packet.writeDouble(event.getY());
            packet.writeDouble(event.getZ());
        }
    }

    private static void writeEventPosition(PacketBuilder packet, NetworkActivityEvent event) throws IOException {
        if (event.getBlockEntity() != null) {
            packet.writeBoolean(true);
            packet.writeTileEntity(event.getBlockEntity());
        } else {
            packet.writeBoolean(false);
            packet.writeUTF(event.getWorld().dimension().location().toString());
            packet.writeDouble(event.getX());
            packet.writeDouble(event.getY());
            packet.writeDouble(event.getZ());
        }
    }
}
