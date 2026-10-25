package li.cil.oc.common;

import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.common.blockentity.traits.BaseBlockEntity;
import li.cil.oc.server.machine.Callbacks;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;

/** Cleans server resources when a world is unloaded. */
final class WorldLifecycleEvents {
    private WorldLifecycleEvents() {}

    static void disposeServerWorld(ServerLevel level) {
        var chunkMap = level.getChunkSource().chunkMap;
        for (ChunkHolder holder : chunkMap.getChunks()) {
            LevelChunk chunk = holder.getTickingChunk();
            if (chunk != null) {
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof BaseBlockEntity baseBlockEntity) baseBlockEntity.dispose();
                }
            }
        }

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof MachineHost host) host.machine().stop();
        }

        Callbacks.clear();
    }
}