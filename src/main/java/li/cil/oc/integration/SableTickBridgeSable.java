package li.cil.oc.integration;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import li.cil.oc.common.blockentity.Rack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;

/** Loaded reflectively only when Sable is installed. */
public final class SableTickBridgeSable {
    private SableTickBridgeSable() {}

    public static void tick(MinecraftServer server) {
        var desired = new HashSet<SableTickBridge.TickTicket>();
        for (var level : server.getAllLevels()) {
            var container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
            if (container == null) continue;
            for (var subLevel : container.collectForceLoadedSubLevels()) {
                for (var holder : subLevel.getPlot().getLoadedChunks()) {
                    var chunk = holder.getChunk();
                    if (chunk == null) continue;
                    for (var entry : chunk.getBlockEntities().entrySet()) {
                        if (entry.getValue() instanceof Rack) {
                            var pos = entry.getKey();
                            desired.add(new SableTickBridge.TickTicket(level, pos.immutable(), new ChunkPos(pos)));
                            break;
                        }
                    }
                }
            }
        }
        SableTickBridge.reconcile(desired);
    }
}
