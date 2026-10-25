package li.cil.oc.common.component;

import com.google.common.base.Strings;
import li.cil.oc.client.ComponentTracker;
import li.cil.oc.client.TextBufferClientPacketSender;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/** Client-side registration state for live screen text buffers. */
public final class TextBufferClientRegistry {
    private static final List<TextBuffer> CLIENT_BUFFERS = new ArrayList<>();

    private TextBufferClientRegistry() {}

    public static void onChunkUnloaded(ChunkEvent.Unload event) {
        var chunk = event.getChunk();
        ChunkPos chunkPos = chunk.getPos();
        Iterator<TextBuffer> iterator = CLIENT_BUFFERS.iterator();
        while (iterator.hasNext()) {
            TextBuffer buffer = iterator.next();
            BlockPosition position = BlockPositionsJava.apply(buffer.host());
            Level hostLevel = buffer.host().getEnvironmentLevel();
            boolean keep = !Objects.equals(hostLevel, event.getLevel())
                || (position.x() >> 4) != chunkPos.x || (position.z() >> 4) != chunkPos.z;
            if (!keep) {
                ComponentTracker.INSTANCE.remove(hostLevel, buffer);
                iterator.remove();
            }
        }
    }

    public static void onWorldUnload(LevelEvent.Unload event) {
        Iterator<TextBuffer> iterator = CLIENT_BUFFERS.iterator();
        while (iterator.hasNext()) {
            TextBuffer buffer = iterator.next();
            Level hostLevel = buffer.host().getEnvironmentLevel();
            if (Objects.equals(hostLevel, event.getLevel())) {
                ComponentTracker.INSTANCE.remove(hostLevel, buffer);
                iterator.remove();
            }
        }
    }

    public static void registerClientBuffer(TextBuffer buffer) {
        registerClientBuffer(buffer, buffer.host().getEnvironmentLevel());
    }

    public static boolean registerClientBuffer(TextBuffer buffer, Level level) {
        if (level == null || Strings.isNullOrEmpty(buffer.proxy().nodeAddress())) return false;

        Level hostLevel = buffer.host().getEnvironmentLevel();
        if (hostLevel != null && !Objects.equals(hostLevel, level)) {
            ComponentTracker.INSTANCE.remove(hostLevel, buffer);
        }

        ComponentTracker.INSTANCE.remove(level, buffer);
        ComponentTracker.INSTANCE.add(level, buffer.proxy().nodeAddress(), buffer);

        if (!CLIENT_BUFFERS.contains(buffer)) CLIENT_BUFFERS.add(buffer);

        TextBufferClientPacketSender.sendTextBufferInit(buffer.proxy().nodeAddress());
        return true;
    }

    public static void unregisterClientBuffer(TextBuffer buffer, Level level) {
        if (level == null) return;

        ComponentTracker.INSTANCE.remove(level, buffer);
        Level hostLevel = buffer.host().getEnvironmentLevel();
        if (hostLevel != null && !Objects.equals(hostLevel, level)) {
            ComponentTracker.INSTANCE.remove(hostLevel, buffer);
        }
        CLIENT_BUFFERS.remove(buffer);
    }
}
