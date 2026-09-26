package li.cil.oc.integration.multipart

import codechicken.multipart.api.MultipartType
import codechicken.multipart.api.part.MultiPart
import codechicken.multipart.minecraft.McStatePart
import li.cil.oc.common.block.CableHelper
import li.cil.oc.common.EventHandler
import li.cil.oc.common.block.property.PropertyCableConnection
import li.cil.oc.common.init.OCBlocks
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.{CollisionContext, VoxelShape}
import codechicken.multipart.util.PartRayTraceResult

/** Audio cable part. Loaded only by the optional CB Multipart integration. */
final class MultipartAudioCablePart(initialState: BlockState = OCBlocks.AudioCable.get.defaultBlockState()) extends McStatePart(initialState) {
  override def getType: MultipartType[_] = MultipartIntegration.audioCableMultipartType
  override def defaultBlockState: BlockState = OCBlocks.AudioCable.get.defaultBlockState()
  override def getCurrentState: BlockState = {
    updateConnections()
    state
  }
  override def getDropStack: ItemStack = new ItemStack(OCBlocks.AudioCable.get)
  override def getCloneStack(hit: PartRayTraceResult, player: Player): ItemStack = getDropStack
  override def getCloneStack(hit: PartRayTraceResult): ItemStack = getDropStack
  override def getShape(context: CollisionContext): VoxelShape = state.getShape(level, pos, context)
  override def getCollisionShape(context: CollisionContext): VoxelShape = state.getCollisionShape(level, pos, context)
  // AudioCable also inherits the full-block default occlusion shape; use its actual
  // connection-dependent cable geometry when it participates in a multipart tile.
  override def getRenderOcclusionShape: VoxelShape = state.getShape(level, pos, CollisionContext.empty())

  override def onAdded(): Unit = {
    super.onAdded()
    updateConnections()
    refreshAdjacentBlocks()
  }

  override def onPartChanged(other: MultiPart): Unit = {
    super.onPartChanged(other)
    updateConnections()
    refreshAdjacentBlocks()
  }

  override def onNeighborBlockChanged(changedPos: BlockPos): Unit = {
    super.onNeighborBlockChanged(changedPos)
    updateConnections()
  }

  override def onChunkLoad(chunk: net.minecraft.world.level.chunk.LevelChunk): Unit = {
    super.onChunkLoad(chunk)
    refreshAdjacentBlocks()
  }

  private def refreshAdjacentBlocks(): Unit = if (hasLevel && !level.isClientSide)
    EventHandler.scheduleServer(() => if (hasLevel && MultipartColorLookup.isAudioCable(level, pos))
      level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock))

  private def updateConnections(): Unit = if (hasLevel) {
    val nextState = Direction.values.foldLeft(OCBlocks.AudioCable.get.defaultBlockState()) { (current, side) =>
      val neighborPos = pos.relative(side)
      val neighbor = level.getBlockEntity(neighborPos)
      val canPassMultipart = MultipartColorLookup.canAudioConnectFromSide(level, pos, side) &&
        MultipartColorLookup.canAudioConnectFromSide(level, neighborPos, side.getOpposite)
      val shape = if (canPassMultipart && (neighbor.isInstanceOf[li.cil.oc.common.blockentity.AudioCable] || MultipartColorLookup.isAudioCable(level, neighborPos)))
        PropertyCableConnection.Shape.CABLE
      else if (canPassMultipart && (neighbor.isInstanceOf[li.cil.oc.common.blockentity.Speaker] || neighbor.isInstanceOf[li.cil.oc.common.blockentity.TapeDrive]))
        PropertyCableConnection.Shape.DEVICE
      else PropertyCableConnection.Shape.NONE
      CableHelper.helperSetCableShapeState(current, side, shape)
    }
    if (state != nextState) {
      state = nextState
      if (hasTile) tile.notifyShapeChange()
      if (hasLevel && !level.isClientSide) sendUpdate(out => writeDesc(out))
    }
  }
}
