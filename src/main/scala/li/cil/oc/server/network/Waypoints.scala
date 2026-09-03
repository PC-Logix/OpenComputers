package li.cil.oc.server.network

import li.cil.oc.Settings
import li.cil.oc.common.blockentity.Waypoint
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.RTree
import li.cil.oc.util.SableCompat
import net.neoforged.bus.api.SubscribeEvent

import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable
import net.minecraft.world.level.Level
import net.minecraft.resources.ResourceKey
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.level.ChunkEvent

object Waypoints {
  val dimensions = mutable.Map.empty[ResourceKey[Level], RTree[Waypoint]]

  @SubscribeEvent
  def onWorldUnload(e: LevelEvent.Unload): Unit = {
    if (!e.getLevel.isClientSide) {
      e.getLevel match {
        case level: Level => dimensions.remove(level.dimension)
        case _ =>
      }
    }
  }

  @SubscribeEvent
  def onWorldLoad(e: LevelEvent.Load): Unit = {
    if (!e.getLevel.isClientSide) {
      e.getLevel match {
        case level: Level => dimensions.remove(level.dimension)
        case _ =>
      }
    }
  }

  // Safety clean up, in case some tile entities didn't properly leave the net.
  @SubscribeEvent
  def onChunkUnloaded(e: ChunkEvent.Unload): Unit = {
    e.getChunk.getBlockEntitiesPos.map(e.getChunk.getBlockEntity).foreach {
      case waypoint: Waypoint => remove(waypoint)
      case _ =>
    }
  }

  def add(waypoint: Waypoint): Unit = if (!waypoint.isRemoved && waypoint.getEnvironmentLevel != null && !waypoint.getEnvironmentLevel.isClientSide) {
    dimensions.getOrElseUpdate(dimension(waypoint), new RTree[Waypoint](Settings.get.rTreeMaxEntries)(coordinate)).add(waypoint)
  }

  def remove(waypoint: Waypoint): Unit = if (waypoint.getEnvironmentLevel != null && !waypoint.getEnvironmentLevel.isClientSide) {
    dimensions.get(dimension(waypoint)) match {
      case Some(set) => set.remove(waypoint)
      case _ =>
    }
  }

  def findWaypoints(pos: BlockPosition, range: Double): Iterable[Waypoint] = {
    dimensions.get(pos.world.get.dimension) match {
      case Some(set) =>
        val physical = SableCompat.physicalPosition(pos.world.orNull, pos.toVec3)
        val bounds = new net.minecraft.world.phys.AABB(physical.x, physical.y, physical.z,
          physical.x + 1, physical.y + 1, physical.z + 1).
          inflate(range * 0.5, range * 0.5, range * 0.5)
        set.query((bounds.minX, bounds.minY, bounds.minZ), (bounds.maxX, bounds.maxY, bounds.maxZ))
      case _ => Iterable.empty
    }
  }

  private def dimension(waypoint: Waypoint) = waypoint.getEnvironmentLevel.dimension

  private def coordinate(waypoint: Waypoint) = {
    val position = SableCompat.physicalPosition(waypoint.getEnvironmentLevel,
      new net.minecraft.world.phys.Vec3(waypoint.x + 0.5, waypoint.y + 0.5, waypoint.z + 0.5))
    (position.x, position.y, position.z)
  }
}
