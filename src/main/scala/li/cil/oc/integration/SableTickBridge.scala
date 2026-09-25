package li.cil.oc.integration

import li.cil.oc.OpenComputers
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.neoforged.neoforge.common.world.chunk.{RegisterTicketControllersEvent, TicketController, TicketHelper}

import scala.collection.mutable
import scala.jdk.CollectionConverters._

/** Lets NeoForge know that Sable's own construction tickets require this level to keep ticking. */
object SableTickBridge {
  final case class TickTicket(level: ServerLevel, owner: BlockPos, chunk: ChunkPos)

  private val activeTickets = mutable.Set.empty[TickTicket]
  private var ticketController: TicketController = _
  private var sableTickModule: AnyRef = _
  private var sableTickMethod: java.lang.reflect.Method = _
  private var sableTickUnavailable = false

  def onRegisterTicketControllers(event: RegisterTicketControllersEvent): Unit = {
    ticketController = new TicketController(
      ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "sable_sublevel_tick"),
      (_: ServerLevel, helper: TicketHelper) => helper.getBlockTickets.keySet().asScala.foreach(owner => helper.removeAllTickets(owner))
    )
    event.register(ticketController)
  }

  /** Called before the server ticks its levels, so the forced-chunk check sees the bridge ticket. */
  def tick(server: MinecraftServer): Unit = {
    if (!Mods.Sable.isModAvailable || ticketController == null || sableTickUnavailable) return

    try {
      if (sableTickMethod == null) {
        val bridgeClass = Class.forName("li.cil.oc.integration.SableTickBridgeSable$")
        sableTickModule = bridgeClass.getField("MODULE$").get(null).asInstanceOf[AnyRef]
        sableTickMethod = bridgeClass.getMethod("tick", classOf[MinecraftServer])
      }
      sableTickMethod.invoke(sableTickModule, server)
    } catch {
      case error: java.lang.reflect.InvocationTargetException =>
        sableTickUnavailable = true
        OpenComputers.log.warn("Sable tick compatibility bridge could not inspect loaded constructions.", error.getCause)
      case error: Throwable =>
        sableTickUnavailable = true
        OpenComputers.log.warn("Sable tick compatibility bridge could not be loaded; continuing without it.", error)
    }
  }

  def reconcile(desiredTickets: Iterable[TickTicket]): Unit = {
    val desired = desiredTickets.toSet
    activeTickets.filterNot(desired.contains).foreach(removeTicket)
    desired.filterNot(activeTickets.contains).foreach(addTicket)
    activeTickets.clear()
    activeTickets ++= desired
  }

  private def addTicket(ticket: TickTicket): Unit = try {
    ticketController.forceChunk(ticket.level, ticket.owner, ticket.chunk.x, ticket.chunk.z, true, true)
  } catch {
    case error: Throwable => OpenComputers.log.warn("Failed to keep a Sable rack's parent level ticking.", error)
  }

  private def removeTicket(ticket: TickTicket): Unit = try {
    ticketController.forceChunk(ticket.level, ticket.owner, ticket.chunk.x, ticket.chunk.z, false, true)
  } catch {
    case error: Throwable => OpenComputers.log.warn("Failed to release a Sable rack's parent-level tick ticket.", error)
  }
}
