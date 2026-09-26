package li.cil.oc.integration.multipart

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.multipart.api.MultipartType
import codechicken.multipart.api.part.MultiPart
import codechicken.multipart.api.part.redstone.RedstonePart
import codechicken.multipart.minecraft.McStatePart
import codechicken.multipart.util.PartRayTraceResult
import io.netty.buffer.Unpooled
import li.cil.oc.{Settings}
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.init.OCBlocks
import li.cil.oc.util.ExtendedAABB._
import net.minecraft.core.{BlockPos, Direction, HolderLookup}
import net.minecraft.nbt.CompoundTag
import net.minecraft.sounds.{SoundEvents, SoundSource}
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.{CollisionContext, Shapes, VoxelShape}

import scala.jdk.CollectionConverters._

/** A print keeps its own design and state when sharing a CB Multipart block. */
final class MultipartPrintPart(
  initialState: BlockState = OCBlocks.Print.get.defaultBlockState(),
  original: Option[li.cil.oc.common.blockentity.Print] = None)
  extends McStatePart(initialState) with RedstonePart {

  var data = new PrintData()
  var facing: Direction = Direction.SOUTH
  var active = false
  private var updatingInput = false

  original.foreach { print =>
    data = print.data
    facing = print.facing
    active = print.state
  }

  override def getType: MultipartType[_] = MultipartIntegration.printMultipartType
  override def defaultBlockState: BlockState = OCBlocks.Print.get.defaultBlockState()
  override def getDropStack: ItemStack = data.createItemStack()
  override def getCloneStack(hit: PartRayTraceResult, player: Player): ItemStack = getDropStack
  override def getCloneStack(hit: PartRayTraceResult): ItemStack = getDropStack

  def shapes: Iterable[PrintData.Shape] = if (active) data.stateOn else data.stateOff

  private def printedShape: VoxelShape = shapes.foldLeft(Shapes.empty()) { (current, shape) =>
    Shapes.or(current, Shapes.create(shape.bounds.rotateTowards(facing)))
  }

  override def getShape(context: CollisionContext): VoxelShape = {
    val shape = printedShape
    if (shape.isEmpty) Shapes.box(0.4375, 0.4375, 0.4375, 0.5625, 0.5625, 0.5625)
    else shape
  }
  override def getCollisionShape(context: CollisionContext): VoxelShape =
    if (if (active) data.noclipOn else data.noclipOff) Shapes.empty() else printedShape
  override def getOcclusionShape: VoxelShape = printedShape
  override def getRenderOcclusionShape: VoxelShape = printedShape
  override def getLightEmission: Int = data.lightLevel

  override def canConnectRedstone(side: Int): Boolean = true
  override def strongPowerLevel(side: Int): Int = weakPowerLevel(side)
  override def weakPowerLevel(side: Int): Int = if (data.emitRedstone(active)) data.redstoneLevel else 0

  override def useWithoutItem(player: Player, hit: PartRayTraceResult): InteractionResult = {
    if (!data.hasActiveState || (active && data.isButtonMode)) return super.useWithoutItem(player, hit)
    if (level.isClientSide) return InteractionResult.SUCCESS
    if (toggleState()) InteractionResult.CONSUME else InteractionResult.PASS
  }

  override def scheduledTick(): Unit = if (active && data.isButtonMode && hasLevel && !level.isClientSide)
    toggleState()

  private def toggleState(): Boolean = {
    if (!hasTile || !hasLevel || level.isClientSide) return false
    val previous = active
    active = !active
    if (!tile.canReplacePart(this, this)) {
      active = previous
      return false
    }
    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
      0.3f, if (active) 0.6f else 0.5f)
    tile.setChanged()
    tile.notifyShapeChange()
    tile.notifyPartChange(this)
    sendUpdate(out => writeDesc(out))
    if (active && data.isButtonMode) scheduleTick(20)
    true
  }

  private def updateFromRedstone(): Unit = {
    if (updatingInput || !hasLevel || !hasTile || level.isClientSide || data.emitRedstone || !data.hasActiveState) return
    updatingInput = true
    try {
      val otherParts = tile.getPartList.asScala.collect {
        case part: RedstonePart if part != this => Direction.values.map(side => part.weakPowerLevel(side.ordinal())).max
      }
      val input = level.getBestNeighborSignal(pos) max otherParts.foldLeft(0)(_ max _)
      if (active != (input > 1)) toggleState()
    }
    finally updatingInput = false
  }

  override def onPartChanged(other: MultiPart): Unit = {
    super.onPartChanged(other)
    updateFromRedstone()
  }
  override def onNeighborBlockChanged(changedPos: BlockPos): Unit = {
    super.onNeighborBlockChanged(changedPos)
    updateFromRedstone()
  }
  override def onWorldJoin(): Unit = {
    super.onWorldJoin()
    updateFromRedstone()
  }

  private final val DataTag = Settings.namespace + "data"
  private final val FacingTag = Settings.namespace + "facing"
  private final val ActiveTag = Settings.namespace + "state"

  override def save(tag: CompoundTag, registry: HolderLookup.Provider): Unit = {
    super.save(tag, registry)
    val printTag = new CompoundTag()
    data.saveData(printTag, registry)
    tag.put(DataTag, printTag)
    tag.putInt(FacingTag, facing.ordinal())
    tag.putBoolean(ActiveTag, active)
  }
  override def load(tag: CompoundTag, registry: HolderLookup.Provider): Unit = {
    super.load(tag, registry)
    data.loadData(tag.getCompound(DataTag), registry)
    facing = Direction.from3DDataValue(tag.getInt(FacingTag))
    active = tag.getBoolean(ActiveTag)
  }
  override def writeDesc(out: MCDataOutput): Unit = {
    super.writeDesc(out)
    val buffer = Unpooled.buffer()
    try {
      PrintData.STREAM_CODEC.encode(buffer, data)
      out.writeByteBuf(buffer)
    }
    finally buffer.release()
    out.writeDirection(facing)
    out.writeBoolean(active)
  }
  override def readDesc(in: MCDataInput): Unit = {
    super.readDesc(in)
    val buffer = in.readByteBuf()
    try data = PrintData.STREAM_CODEC.decode(buffer)
    finally buffer.release()
    facing = in.readDirection()
    active = in.readBoolean()
    if (hasTile) tile.markRender()
  }
}
