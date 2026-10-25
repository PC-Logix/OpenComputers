package li.cil.oc.client;

import li.cil.oc.common.PacketHandlerBase.PacketParser;
import li.cil.oc.common.blockentity.Projector;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Buffers full projector frames that arrive before their client block entities are created. */
public final class ClientProjectorFrameHandler {
    private record Key(ResourceLocation dimension, int blockX, int blockY, int blockZ) {}

    private static final Map<Key, PendingProjectorFrame> pendingFrames = new LinkedHashMap<>();

    private ClientProjectorFrameHandler() {}

    public static void clearPendingFrames() {
        synchronized (pendingFrames) {
            pendingFrames.clear();
        }
    }

    public static void applyPendingFrames() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        List<Map.Entry<Key, PendingProjectorFrame>> frames = new ArrayList<>();
        synchronized (pendingFrames) {
            for (Map.Entry<Key, PendingProjectorFrame> entry : pendingFrames.entrySet()) {
                frames.add(Map.entry(entry.getKey(), entry.getValue()));
            }
        }

        List<Key> applied = new ArrayList<>();
        for (Map.Entry<Key, PendingProjectorFrame> entry : frames) {
            PendingProjectorFrame frame = entry.getValue();
            Projector projector = findProjector(frame, player);
            if (projector != null) {
                projector.clientFrame(frame.x(), frame.y(), frame.width(), frame.height(), frame.data());
                applied.add(entry.getKey());
            }
        }
        if (!applied.isEmpty()) {
            synchronized (pendingFrames) {
                for (Key key : applied) pendingFrames.remove(key);
            }
        }
    }

    public static void receiveFrame(PacketParser packet) throws IOException {
        ResourceLocation dimension = ResourceLocation.tryParse(packet.readUTF());
        int blockX = packet.readInt();
        int blockY = packet.readInt();
        int blockZ = packet.readInt();
        int x = packet.readInt();
        int y = packet.readInt();
        int width = packet.readInt();
        int height = packet.readInt();
        boolean valid = dimension != null && x >= 0 && y >= 0 && width >= 0 && height >= 0
            && x + width <= Projector.Width && y + height <= Projector.Height
            && (long) width * height <= (long) Projector.Width * Projector.Height;
        if (!valid) return;

        int[] data = new int[width * height];
        for (int index = 0; index < data.length; index++) data[index] = packet.readInt();
        PendingProjectorFrame frame = new PendingProjectorFrame(dimension, blockX, blockY, blockZ, x, y, width, height, data);
        Player player = packet.player;
        Projector projector = findProjector(frame, player);
        if (projector != null) {
            projector.clientFrame(x, y, width, height, data);
        } else {
            Key key = new Key(dimension, blockX, blockY, blockZ);
            synchronized (pendingFrames) {
                pendingFrames.put(key, frame);
            }
        }
    }

    private static Projector findProjector(PendingProjectorFrame frame, Player player) {
        Level world = player.level();
        if (!world.dimension().location().equals(frame.dimension())) return null;

        BlockPos position = new BlockPos(frame.blockX(), frame.blockY(), frame.blockZ());
        if (!world.isLoaded(position)) return null;
        BlockEntity blockEntity = world.getBlockEntity(position);
        return blockEntity instanceof Projector projector ? projector : null;
    }
}
