package li.cil.oc.integration;

import li.cil.oc.OpenComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/** Lets NeoForge know that Sable's construction tickets require the parent level to keep ticking. */
public final class SableTickBridge {
    public record TickTicket(ServerLevel level, BlockPos owner, ChunkPos chunk) {}

    private static final Set<TickTicket> ACTIVE_TICKETS = new HashSet<>();
    private static TicketController ticketController;
    private static Method sableTickMethod;
    private static boolean sableTickUnavailable;

    private SableTickBridge() {}

    public static void onRegisterTicketControllers(RegisterTicketControllersEvent event) {
        ticketController = new TicketController(
                ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "sable_sublevel_tick"),
                (level, helper) -> {
                    for (var owner : helper.getBlockTickets().keySet()) helper.removeAllTickets(owner);
                });
        event.register(ticketController);
    }

    /** Called before server levels tick, so forced-chunk checks see the bridge ticket. */
    public static void tick(MinecraftServer server) {
        if (!Mods.Sable().isModAvailable() || ticketController == null || sableTickUnavailable) return;
        try {
            if (sableTickMethod == null) {
                var bridgeClass = Class.forName("li.cil.oc.integration.SableTickBridgeSable");
                sableTickMethod = bridgeClass.getMethod("tick", MinecraftServer.class);
            }
            sableTickMethod.invoke(null, server);
        } catch (InvocationTargetException error) {
            sableTickUnavailable = true;
            OpenComputers.log().warn("Sable tick compatibility bridge could not inspect loaded constructions.", error.getCause());
        } catch (Throwable error) {
            sableTickUnavailable = true;
            OpenComputers.log().warn("Sable tick compatibility bridge could not be loaded; continuing without it.", error);
        }
    }

    public static void reconcile(Iterable<TickTicket> desiredTickets) {
        Set<TickTicket> desired = new HashSet<>();
        desiredTickets.forEach(desired::add);
        for (var ticket : ACTIVE_TICKETS) if (!desired.contains(ticket)) removeTicket(ticket);
        for (var ticket : desired) if (!ACTIVE_TICKETS.contains(ticket)) addTicket(ticket);
        ACTIVE_TICKETS.clear();
        ACTIVE_TICKETS.addAll(desired);
    }

    private static void addTicket(TickTicket ticket) {
        try {
            ticketController.forceChunk(ticket.level(), ticket.owner(), ticket.chunk().x, ticket.chunk().z, true, true);
        } catch (Throwable error) {
            OpenComputers.log().warn("Failed to keep a Sable rack's parent level ticking.", error);
        }
    }

    private static void removeTicket(TickTicket ticket) {
        try {
            ticketController.forceChunk(ticket.level(), ticket.owner(), ticket.chunk().x, ticket.chunk().z, false, true);
        } catch (Throwable error) {
            OpenComputers.log().warn("Failed to release a Sable rack's parent-level tick ticket.", error);
        }
    }
}
