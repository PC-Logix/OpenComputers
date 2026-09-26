package li.cil.oc.integration.multipart

import codechicken.multipart.block.TileMultipart
import codechicken.multipart.api.NormalOcclusionTest
import codechicken.multipart.api.part.MultiPart
import scala.jdk.CollectionConverters._
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.level.BlockGetter

/** Loaded only when the optional CB Multipart mod is installed. */
private[multipart] object MultipartColorLookup {
  /** True when an OC cable arm can pass through this multipart cell on the given side. */
  def canConnectFromSide(world: BlockGetter, pos: BlockPos, side: Direction): Boolean =
    canConnectFromSide(world, pos, side, _.isInstanceOf[MultipartCablePart])

  def canAudioConnectFromSide(world: BlockGetter, pos: BlockPos, side: Direction): Boolean =
    canConnectFromSide(world, pos, side, _.isInstanceOf[MultipartAudioCablePart])

  private def canConnectFromSide(world: BlockGetter, pos: BlockPos, side: Direction, isCablePart: MultiPart => Boolean): Boolean = world.getBlockEntity(pos) match {
    case tile: TileMultipart =>
      val cablePath = NormalOcclusionTest.of(li.cil.oc.common.block.Cable.CachedBounds(li.cil.oc.common.block.Cable.mask(side)))
      tile.getPartList.asScala.forall {
        case part if isCablePart(part) => true
        case part => NormalOcclusionTest.test(cablePath, part)
      }
    case _ => true
  }

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
