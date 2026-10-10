package li.cil.oc.common.blockentity

import li.cil.oc.{Constants, Settings, api}
import li.cil.oc.client.renderer.block.PrintModel
import li.cil.oc.common.block.{Print => PrintBlock}
import li.cil.oc.common.blockentity.traits.RedstoneChangedEventArgs
import li.cil.oc.common.init.OCBlocks
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.util.ExtendedAABB
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.core.{BlockPos, Direction, HolderLookup}
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.{SoundEvents, SoundSource}
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.{BooleanOp, Shapes, VoxelShape}
import net.minecraft.world.ticks.ScheduledTick
import net.neoforged.api.distmarker.{Dist, OnlyIn}
import net.neoforged.neoforge.client.model.data.{ModelData, ModelProperty}
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension

import java.util

class Print(pos: BlockPos, blockState: BlockState, val canToggle: Option[() => Boolean], val scheduleUpdate: Option[Int => Unit], val onStateChange: Option[() => Unit])
  extends BlockEntity(BlockEntityTypes.PRINT.get(), pos, blockState) with traits.BaseBlockEntity with traits.RedstoneAware with traits.RotatableBaseBlock
    with IBlockEntityExtension {

  def this(pos: BlockPos, blockState: BlockState) = this(pos, blockState, None, None, None)
  def this(pos: BlockPos, blockState: BlockState, canToggle: () => Boolean, scheduleUpdate: Int => Unit, onStateChange: () => Unit) =
    this(pos, blockState, Option(canToggle), Option(scheduleUpdate), Option(onStateChange))

  _isOutputEnabled = true

  val data = new PrintData()

  var shapeOff = Shapes.block
  var shapeOn = Shapes.block
  var state = false

  def shape = if (state) shapeOn else shapeOff
  def noclip = if (state) data.noclipOn else data.noclipOff
  def shapes = if (state) data.stateOn else data.stateOff

  @OnlyIn(Dist.CLIENT)
  override def getModelData: ModelData =
    ModelData.builder()
      .`with`(PrintModel.PRINT_PROPERTY, this)
      .build()

  def activate(): Boolean = {
    if (data.hasActiveState) {
      if (!state || !data.isButtonMode) {
        toggleState()
        return true
      }
    }
    false
  }

  private def buildValueSet(value: Int): util.Map[AnyRef, AnyRef] = {
    val map: util.Map[AnyRef, AnyRef] = new util.HashMap[AnyRef, AnyRef]()
    Direction.values.foreach {
      side => map.put(Int.box(side.ordinal()), Int.box(value))
    }
    map
  }

  def toggleState(): Unit = {
    if (canToggle.fold(true)(_.apply())) {
      state = !state
      getLevel.playSound(null, getBlockPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, if (state) 0.6F else 0.5F)
      getLevel.sendBlockUpdated(getBlockPos, getLevel.getBlockState(getBlockPos), getLevel.getBlockState(getBlockPos), 3)
      updateRedstone()
      if (state && data.isButtonMode) {
        val block = OCBlocks.Print.get()
        val delay = block.tickRate(getLevel)
        scheduleUpdate match {
          case Some(callback) => callback(delay)
          case _ if !getLevel.isClientSide =>
            val serverLevel = getLevel.asInstanceOf[ServerLevel]
            val triggerTime = serverLevel.getGameTime + delay
            serverLevel.getBlockTicks.schedule(new ScheduledTick(block, getBlockPos, triggerTime, serverLevel.getGameTime))
          case _ =>
        }
      }
      onStateChange.foreach(_.apply())
    }
  }

  private def convertShape(state: Iterable[PrintData.Shape]): VoxelShape = if (!state.isEmpty) {
    state.foldLeft(Shapes.empty)((curr, s) => {
      val voxel = Shapes.create(ExtendedAABB.rotateTowards(s.bounds, facing))
      Shapes.joinUnoptimized(curr, voxel, BooleanOp.OR)
    }).optimize()
  }
  else Shapes.block

  def updateShape(): Unit = {
    shapeOff = convertShape(data.stateOff)
    shapeOn = convertShape(data.stateOn)
  }

  def updateRedstone(): Unit = {
    if (data.emitRedstone) {
      setOutput(buildValueSet(if (data.emitRedstone(state)) data.redstoneLevel else 0))
    }
  }

  override protected def onRedstoneInputChanged(args: RedstoneChangedEventArgs): Unit = {
    val newState = args.newValue > 0
    if (!data.emitRedstone && data.hasActiveState && state != newState) {
      toggleState()
    }
  }

  override protected def onRotationChanged(): Unit = {
    super.onRotationChanged()
    updateShape()
  }

  // ----------------------------------------------------------------------- //

  private final val DataTag = Settings.namespace + "data"
  private final val StateTag = Settings.namespace + "state"

  override def loadForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadForServer(nbt, provider)
    data.loadData(nbt.getCompound(DataTag), provider)
    state = nbt.getBoolean(StateTag)
    updateShape()
  }

  override def saveForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.saveForServer(nbt, provider)
    nbt.setNewCompoundTag(DataTag, (nbt: CompoundTag) => data.saveData(nbt, provider))
    nbt.putBoolean(StateTag, state)
  }

  override def loadForClient(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadForClient(nbt, provider)
    data.loadData(nbt.getCompound(DataTag), provider)
    state = nbt.getBoolean(StateTag)
    updateShape()
    if (getLevel != null) {
      getLevel.sendBlockUpdated(getBlockPos, getLevel.getBlockState(getBlockPos), getLevel.getBlockState(getBlockPos), 3)
      if (data.emitLight) getLevel.getLightEngine.checkBlock(getBlockPos)
    }
  }

  override def saveForClient(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.saveForClient(nbt, provider)
    nbt.setNewCompoundTag(DataTag, (nbt: CompoundTag) => data.saveData(nbt, provider))
    nbt.putBoolean(StateTag, state)
  }

  // ----------------------------------------------------------------------- //

  @Deprecated
  override def hasProperty(prop: ModelProperty[_]) = false

  @Deprecated
  override def getData[T](prop: ModelProperty[T]): T = null.asInstanceOf[T]

  @Deprecated
  override def setData[T](prop: ModelProperty[T], value: T): T = null.asInstanceOf[T]
}
