package li.cil.oc.common.event;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.network.Node;
import li.cil.oc.server.component.UpgradeChunkloader;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.event.level.LevelEvent;
import scala.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChunkloaderUpgradeHandler {
    public static final ChunkloaderUpgradeHandler INSTANCE = new ChunkloaderUpgradeHandler();
    private final Map<UUID, RestoredTicket> restoredTickets = new HashMap<>();
    private TicketController ticketController;

    private ChunkloaderUpgradeHandler() {}

    public Option<ChunkPos> claimTicket(String address, Level level) {
        UUID owner = parseAddress(address);
        RestoredTicket ticket = owner == null ? null : restoredTickets.get(owner);
        if (ticket == null || !ticket.dimension().equals(level.dimension())) return Option.empty();
        restoredTickets.remove(owner);
        return Option.apply(ticket.position());
    }

    public void onRegisterTicketControllers(RegisterTicketControllersEvent event) {
        ticketController = new TicketController(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "chunkloader"), this::validateTickets);
        event.register(ticketController);
    }

    private void validateTickets(ServerLevel world, TicketHelper helper) {
        for (var entry : helper.getEntityTickets().entrySet()) {
            UUID owner = entry.getKey();
            var tickets = entry.getValue().ticking();
            if (tickets.size() == 9) {
                Set<ChunkPos> positions = new HashSet<>();
                int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
                int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
                for (long packed : tickets) {
                    ChunkPos position = new ChunkPos(ChunkPos.getX(packed), ChunkPos.getZ(packed));
                    positions.add(position);
                    minX = Math.min(minX, position.x);
                    maxX = Math.max(maxX, position.x);
                    minZ = Math.min(minZ, position.z);
                    maxZ = Math.max(maxZ, position.z);
                }
                boolean valid = maxX - minX == 2 && maxZ - minZ == 2;
                if (valid) {
                    for (int x = minX; x <= maxX && valid; x++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            if (!positions.contains(new ChunkPos(x, z))) {
                                valid = false;
                                break;
                            }
                        }
                    }
                }
                if (valid) {
                    int x = minX + 1, z = minZ + 1;
                    OpenComputers.log().info("Restoring chunk loader ticket for upgrade at chunk ({}, {}) with address {}.", x, z, owner);
                    restoredTickets.put(owner, new RestoredTicket(world.dimension(), new ChunkPos(x, z)));
                }
                else {
                    OpenComputers.log().warn("Chunk loader ticket for {} loads an incorrect shape.", owner);
                    helper.removeAllTickets(owner);
                    restoredTickets.remove(owner);
                }
            }
            else {
                OpenComputers.log().warn("Chunk loader ticket for {} loads {} chunks.", owner, tickets.size());
                helper.removeAllTickets(owner);
                restoredTickets.remove(owner);
            }
        }
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        for (var entry : new ArrayList<>(restoredTickets.entrySet())) {
            UUID owner = entry.getKey();
            RestoredTicket ticket = entry.getValue();
            if (!ticket.dimension().equals(level.dimension())) continue;
            try {
                OpenComputers.log().warn("A chunk loader ticket has been orphaned! Address: {}, position: ({}, {}) in {}. Removing...",
                    owner, ticket.position().x, ticket.position().z, ticket.dimension().location());
                releaseTicket(level, owner.toString(), ticket.position());
            }
            catch (Throwable error) {
                OpenComputers.log().error("Could not release orphaned chunk loader ticket.", error);
            }
            finally {
                restoredTickets.remove(owner);
            }
        }
    }

    @SubscribeEvent
    public void onMove(RobotMoveEvent.Post event) {
        Node machineNode = event.agent.machine().node();
        for (Node node : machineNode.reachableNodes()) {
            if (node.host() instanceof UpgradeChunkloader loader) updateLoadedChunk(loader);
        }
    }

    public void releaseTicket(ServerLevel level, String address, ChunkPos position) {
        UUID owner = parseAddress(address);
        if (owner == null) {
            OpenComputers.log().warn("Address '{}' could not be parsed", address);
            return;
        }
        forceArea(level, owner, position, false);
    }

    public void requestTicket(UpgradeChunkloader loader) {
        if (!(loader.host().getEnvironmentLevel() instanceof ServerLevel level) || loader.ticket().isDefined()) return;
        UUID owner = parseAddress(loader.node().address());
        if (owner == null) return;
        ChunkPos center = chunkOf(loader);
        forceArea(level, owner, center, true);
        loader.ticket_$eq(Option.apply(center));
    }

    public void updateLoadedChunk(UpgradeChunkloader loader) {
        if (!(loader.host().getEnvironmentLevel() instanceof ServerLevel level) || loader.ticket().isEmpty()) return;
        UUID owner = parseAddress(loader.node().address());
        if (owner == null) return;
        ChunkPos center = chunkOf(loader);
        ChunkPos previous = loader.ticket().get();
        if (center.equals(previous)) return;
        Set<ChunkPos> desired = area(center);
        Set<ChunkPos> existing = area(previous);
        for (ChunkPos chunk : existing) {
            if (!desired.contains(chunk)) ticketController.forceChunk(level, owner, chunk.x, chunk.z, false, true);
        }
        for (ChunkPos chunk : desired) {
            if (!existing.contains(chunk)) ticketController.forceChunk(level, owner, chunk.x, chunk.z, true, true);
        }
        loader.ticket_$eq(Option.apply(center));
    }

    private static ChunkPos chunkOf(UpgradeChunkloader loader) {
        BlockPosition position = BlockPositionsJava.apply(loader.host());
        return new ChunkPos(position.x() >> 4, position.z() >> 4);
    }

    private void forceArea(ServerLevel level, UUID owner, ChunkPos center, boolean force) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                ticketController.forceChunk(level, owner, center.x + x, center.z + z, force, true);
            }
        }
    }

    private static Set<ChunkPos> area(ChunkPos center) {
        Set<ChunkPos> chunks = new HashSet<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) chunks.add(new ChunkPos(center.x + x, center.z + z));
        }
        return chunks;
    }

    private static UUID parseAddress(String address) {
        try { return UUID.fromString(address); }
        catch (RuntimeException ignored) { return null; }
    }

    private record RestoredTicket(ResourceKey<Level> dimension, ChunkPos position) {}
}
