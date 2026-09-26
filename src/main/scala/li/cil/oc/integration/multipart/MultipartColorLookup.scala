package li.cil.oc.integration.multipart

import codechicken.multipart.block.TileMultipart
import scala.jdk.CollectionConverters._
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter

/** Loaded only when the optional CB Multipart mod is installed. */
private[multipart] object MultipartColorLookup {
  def cablePart(world: BlockGetter, pos: BlockPos): Option[MultipartCablePart] = world.getBlockEntity(pos) match {
    case tile: TileMultipart => tile.getPartList.asScala.collectFirst { case cable: MultipartCablePart => cable }
    case _ => None
  }

  def isAudioCable(world: BlockGetter, pos: BlockPos): Boolean = world.getBlockEntity(pos) match {
    case tile: TileMultipart => tile.getPartList.asScala.exists(_.isInstanceOf[MultipartAudioCablePart])
    case _ => false
  }

  def cableColor(world: BlockGetter, pos: BlockPos): Option[Int] = world.getBlockEntity(pos) match {
    case tile: TileMultipart => tile.getPartList.asScala.collectFirst { case cable: MultipartCablePart => cable.getColor }
    case _ => None
  }
}
