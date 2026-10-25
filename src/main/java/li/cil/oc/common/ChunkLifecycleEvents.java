package li.cil.oc.common;

import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.internal.Server;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.common.blockentity.Projector;
import li.cil.oc.server.ProjectorPacketSender;
import li.cil.oc.server.machine.Machine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;

import java.io.IOException;

/** Handles machine cleanup and projector updates at chunk lifecycle boundaries. */
final class ChunkLifecycleEvents {
    private ChunkLifecycleEvents() {}

    static void onChunkUnloaded(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof Level level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        for (var blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof MachineHost host) scheduleClose(host);
            else if (blockEntity instanceof Rack rack) {
                for (int slot = 0; slot < rack.getContainerSize(); slot++) {
                    RackMountable mountable = rack.getMountable(slot);
                    if (mountable instanceof Server server && server.machine() != null) server.machine().stop();
                }
            }
        }

        var chunkPos = chunk.getPos();
        AABB bounds = new AABB(
                chunkPos.getMinBlockX(), level.getMinBuildHeight(), chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX(), level.getMaxBuildHeight(), chunkPos.getMaxBlockZ());
        for (Entity entity : level.getEntitiesOfClass(Entity.class, bounds)) {
            if (entity instanceof MachineHost host) scheduleClose(host);
        }
    }

    private static void scheduleClose(MachineHost host) {
        if (host.machine() instanceof Machine machine) EventHandler.scheduleClose(machine);
    }

    static void onChunkSent(ChunkWatchEvent.Sent event) throws IOException {
        for (var blockEntity : event.getChunk().getBlockEntities().values()) {
            if (blockEntity instanceof Projector projector) {
                ProjectorPacketSender.sendFrameToPlayer(projector, event.getPlayer());
            }
        }
    }
}
