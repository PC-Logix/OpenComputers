package li.cil.oc.integration

import dev.ryanhcode.sable.api.sublevel.{ServerSubLevelContainer, SubLevelContainer}
import li.cil.oc.common.blockentity.Rack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos

import scala.collection.mutable
import scala.jdk.CollectionConverters._

/** Loaded reflectively only when Sable is installed. */
object SableTickBridgeSable {
  def tick(server: MinecraftServer): Unit = {
    val desired = mutable.Set.empty[SableTickBridge.TickTicket]
    server.getAllLevels.asScala.foreach {
      case level: ServerLevel =>
        val container = SubLevelContainer.getContainer(level).asInstanceOf[ServerSubLevelContainer]
        if (container != null) {
          container.collectForceLoadedSubLevels.asScala.foreach { subLevel =>
            subLevel.getPlot.getLoadedChunks.asScala.foreach { holder =>
              Option(holder.getChunk).foreach { chunk =>
                chunk.getBlockEntities.asScala.collectFirst { case (pos, _: Rack) => pos }.foreach { pos =>
                  desired += SableTickBridge.TickTicket(level, pos.immutable(), new ChunkPos(pos))
                }
              }
            }
          }
        }
      case _ =>
    }

    SableTickBridge.reconcile(desired)
  }
}
