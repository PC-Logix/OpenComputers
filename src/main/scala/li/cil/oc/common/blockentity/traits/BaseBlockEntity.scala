package li.cil.oc.common.blockentity.traits

import li.cil.oc.api.Persistable
import li.cil.oc.api.datacomponents.{MutableNbtComponentHolder, NbtComponentHolder}
import li.cil.oc.{OpenComputers, Settings}
import li.cil.oc.client.Sound
import li.cil.oc.common.SaveHandler
import li.cil.oc.util.{BlockPosition, SideTracker}
import net.minecraft.core.HolderLookup
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.core.component.{DataComponentHolder, DataComponentMap, DataComponentPatch, DataComponentType, PatchedDataComponentMap}
import net.minecraft.nbt.{CompoundTag, NbtOps}
import net.minecraft.network.Connection
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.neoforged.api.distmarker.{Dist, OnlyIn}
import net.neoforged.neoforge.client.model.data.ModelProperty
import net.neoforged.neoforge.common.MutableDataComponentHolder
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

import java.util.function.UnaryOperator

trait BaseBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
  private final val IsServerDataTag = Settings.namespace + "isServerData"

  // Create keeps captured block entities off-world while a contraption moves.
  // These helpers live here because Rotatable and RedstoneAware are sibling
  // traits, not children of Environment.
  protected var movingLevel: Level = null
  protected var movingPosition: Vec3 = null
  protected var movingRotation: UnaryOperator[Vec3] = null
  protected var movingState: BlockState = null

  def isMoving: Boolean = movingPosition != null

  def beginMoving(level: Level, position: Vec3, rotation: UnaryOperator[Vec3], state: BlockState): Unit = {
    movingLevel = level
    movingPosition = position
    movingRotation = rotation
    movingState = state
    initialize()
  }

  def updateMovingPosition(position: Vec3, rotation: UnaryOperator[Vec3], state: BlockState): Unit = {
    movingPosition = position
    movingRotation = rotation
    movingState = state
  }

  def movingBlockPos: BlockPos = if (movingPosition == null) getBlockPos else BlockPos.containing(movingPosition)

  def movingBlockState: BlockState = if (movingState == null) getBlockState else movingState

  def movingDirection(side: Direction): Direction = {
    if (movingRotation == null) side
    else {
      val vector = movingRotation.apply(new Vec3(side.getStepX, side.getStepY, side.getStepZ))
      Direction.getNearest(vector.x, vector.y, vector.z)
    }
  }

  def movingNeighbor(side: Direction): BlockPos = movingBlockPos.relative(movingDirection(side))

  def endMoving(): Unit = {
    movingLevel = null
    movingPosition = null
    movingRotation = null
    movingState = null
  }

  def x: Int = getBlockPos.getX

  def y: Int = getBlockPos.getY

  def z: Int = getBlockPos.getZ

  def position = BlockPosition(x, y, z, getLevel)

  def isClient: Boolean = !isServer

  def isServer: Boolean = if (getLevel != null) !getLevel.isClientSide else SideTracker.isServer

  // ----------------------------------------------------------------------- //

  def updateEntity(): Unit = {
    if (Settings.get.periodicallyForceLightUpdate && getLevel.getGameTime % 40 == 0 && getBlockState.getLightEmission(getLevel, getBlockPos) > 0) {
      getLevel.sendBlockUpdated(getBlockPos, getLevel.getBlockState(getBlockPos), getLevel.getBlockState(getBlockPos), 3)
    }
  }

  override def clearRemoved(): Unit = {
    super.clearRemoved()
    initialize()
  }

  override def setRemoved(): Unit = {
    super.setRemoved()
    dispose()
  }

  override def onChunkUnloaded(): Unit = {
    super.onChunkUnloaded()
    try dispose() catch {
      case t: Throwable => OpenComputers.log.error("Failed properly disposing a block entity, things may leak and or break.", t)
    }
  }

  protected def initialize(): Unit = {
  }

  def dispose(): Unit = {
    if (isClient) {
      // Note: chunk unload is handled by sound via event handler.
      Sound.stopLoop(this)
    }
  }

  // ----------------------------------------------------------------------- //

  def loadForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {}

  def saveForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    nbt.putBoolean(IsServerDataTag, true)
  }

  def loadForClient(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {}

  def saveForClient(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    nbt.putBoolean(IsServerDataTag, false)
  }

  def loadComponentsCommon(holder: DataComponentHolder): Unit = {}
  def saveComponentsCommon(holder: MutableDataComponentHolder): Unit = {}
  def loadComponentsForServer(holder: DataComponentHolder): Unit = {}
  def saveComponentsForServer(holder: MutableDataComponentHolder): Unit = {}

  @OnlyIn(Dist.CLIENT)
  def loadComponentsForClient(holder: DataComponentHolder): Unit = {}
  @OnlyIn(Dist.CLIENT)
  def saveComponentsForClient(holder: MutableDataComponentHolder): Unit = {}

  // ----------------------------------------------------------------------- //

  override def loadAdditional(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadAdditional(nbt, provider)
    if (isServer || nbt.getBoolean(IsServerDataTag)) {
      loadForServer(nbt, provider)
    } else {
      loadForClient(nbt, provider)
    }
  }

  override def loadWithComponents(tag: CompoundTag, registries: HolderLookup.Provider): Unit = {
    super.loadWithComponents(tag, registries)
    // components are loaded here
    // Client update tags carry the render-only component snapshot in NBT;
    // persistent block entity components are only available directly when
    // loading on the server.
    val holder: DataComponentHolder =
      if (isServer) {
        // OC's data components are persistent block-entity state, not merely
        // item-transfer components. Once a block has been saved to disk, read
        // the serialized component patch from its NBT. For a freshly placed
        // block there is no NBT snapshot yet, so fall back to the component
        // map Minecraft applied from the placing ItemStack.
        if (NbtComponentHolder.hasComponents(tag))
          new NbtComponentHolder(tag, registries)
        else
          Persistable.holder(this)
      }
      else new NbtComponentHolder(tag, registries)

    loadComponentsCommon(holder)

    if(isServer) {
      loadComponentsForServer(holder)
    } else {
      loadComponentsForClient(holder)
    }
  }

  override def saveAdditional(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.saveAdditional(nbt, provider)
    save(nbt, provider)

    if (isServer) {
      // Data components on a BlockEntity are not automatically chunk-persistent.
      // Serialize OC's persistent component snapshot into the block entity NBT.
      // saveForServer may itself persist component-backed state into this NBT
      // (notably TextBuffer, whose node address is required to reconnect the
      // screen and locate its external buffer save). Seed this holder from the
      // NBT we have already written instead of starting empty, otherwise this
      // second component write replaces opencomputers:components and silently
      // drops everything written by saveForServer.
      val holder = new MutableNbtComponentHolder(nbt, provider)
      saveComponentsCommon(holder)
      saveComponentsForServer(holder)
      holder.save(nbt, provider)

      // Keep the live BlockEntity component map in sync as well. Minecraft
      // uses this map when transferring block state to an ItemStack.
      val liveHolder = Persistable.holder(this)
      try liveHolder.applyComponents(holder.getComponents)
      finally liveHolder.close()
    }
    else {
      val holder = Persistable.holder(this)
      try {
        saveComponentsCommon(holder)
        saveComponentsForClient(holder)
      } finally {
        holder.close()
      }
    }
  }

  def save(nbt: CompoundTag, provider: HolderLookup.Provider): CompoundTag = {
    if (isServer) {
      saveForServer(nbt, provider)
    }
    nbt
  }

  override def getUpdatePacket: ClientboundBlockEntityDataPacket = {
    ClientboundBlockEntityDataPacket.create(this)
  }

  override def getUpdateTag(provider: HolderLookup.Provider): CompoundTag = {
    val nbt = super.getUpdateTag(provider)

    // See comment on savingForClients variable.
    SaveHandler.savingForClients = true
    try {
      try {
        this match {
          case screen: li.cil.oc.common.blockentity.Screen => screen.saveForClientDirect(nbt, provider)
          case _ => saveForClient(nbt, provider)
        }
      } catch {
        case e: Throwable => OpenComputers.log.warn("There was a problem writing a BlockEntity description packet. Please report this if you see it!", e)
      }

      try {
        // Preserve any component-backed state saveForClient already placed in
        // the update NBT for the same reason as the server save path above.
        val holder = new MutableNbtComponentHolder(nbt, provider)
        saveComponentsCommon(holder)
        saveComponentsForClient(holder)
        holder.save(nbt, provider)
      } catch {
        case e: Throwable => OpenComputers.log.warn("There was a problem writing BlockEntity client components. Please report this if you see it!", e)
      }
    } finally {
      SaveHandler.savingForClients = false
    }

    nbt
  }

  override def onDataPacket(manager: Connection, packet: ClientboundBlockEntityDataPacket, provider: HolderLookup.Provider): Unit = {
    try loadWithComponents(packet.getTag, provider) catch {
      case e: Throwable => OpenComputers.log.warn("There was a problem reading a BlockEntity description packet. Please report this if you see it!", e)
    }
  }
  
  def hasProperty(prop: ModelProperty[_]) = false

  def getData[T](prop: ModelProperty[T]): T = null.asInstanceOf[T]

  def setData[T](prop: ModelProperty[T], value: T): T = null.asInstanceOf[T]

  private def dataComponentMap: PatchedDataComponentMap = components() match {
    case patched: PatchedDataComponentMap => patched
    case notPatched => {
      val patched = new PatchedDataComponentMap(notPatched)
      setComponents(patched)
      patched
    }
  }
}
