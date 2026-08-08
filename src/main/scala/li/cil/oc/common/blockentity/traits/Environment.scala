package li.cil.oc.common.blockentity.traits

import li.cil.oc.Settings
import li.cil.oc.api.network
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.common.EventHandler
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.{Direction, HolderLookup}
import net.minecraft.world.level.Level
import net.neoforged.neoforge.client.model.data.{ModelData, ModelProperty}

trait Environment extends BaseBlockEntity with network.Environment with network.EnvironmentHost {
  protected var isChangeScheduled = false

  override def getEnvironmentLevel: Level = if (movingLevel != null) movingLevel else getLevel

  override def xPosition: Double = if (movingPosition != null) movingPosition.x else x + 0.5

  override def yPosition: Double = if (movingPosition != null) movingPosition.y else y + 0.5

  override def zPosition: Double = if (movingPosition != null) movingPosition.z else z + 0.5

  /** Save a captured environment while Create still owns its off-world NBT. */
  def saveMovingState(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = saveAdditional(nbt, provider)

  /** Remove the temporary OC node before Create places the real block entity. */
  def disposeMoving(): Unit = {
    Option(node).foreach(_.remove())
    this match {
      case sidedEnvironment: SidedEnvironment =>
        for (side <- Direction.values) {
          Option(sidedEnvironment.sidedNode(side)).foreach(_.remove())
        }
      case _ =>
    }
    endMoving()
  }

  override def markChanged(): Unit = if (this.isInstanceOf[Tickable]) isChangeScheduled = true else this.setChanged()

  protected def isConnected: Boolean = node != null && node.address != null && node.network != null

  // ----------------------------------------------------------------------- //

  override protected def initialize(): Unit = {
    super.initialize()
    if (isServer && !isMoving) {
      EventHandler.scheduleServer(this)
    }
  }

  override def updateEntity(): Unit = {
    super.updateEntity()
    if (isChangeScheduled) {
      this.setChanged()
      isChangeScheduled = false
    }
  }

  override def dispose(): Unit = {
    super.dispose()
    if (isServer) {
      Option(node).foreach(_.remove)
      this match {
        case sidedEnvironment: SidedEnvironment => for (side <- Direction.values) {
          Option(sidedEnvironment.sidedNode(side)).foreach(_.remove())
        }
        case _ =>
      }
    }
  }

  // ----------------------------------------------------------------------- //

  private final val NodeTag = Settings.namespace + "node"

  override def loadForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadForServer(nbt, provider)
    if (node != null && node.host == this) {
      node.loadData(nbt.getCompound(NodeTag), provider)
    }
  }

  override def saveForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.saveForServer(nbt, provider)
    if (node != null && node.host == this) {
      nbt.setNewCompoundTag(NodeTag, (nbt: CompoundTag) => node.saveData(nbt, provider))
    }
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: network.Message): Unit = {}

  override def onConnect(node: network.Node): Unit = {}

  override def onDisconnect(node: network.Node): Unit = {
    if (node == this.node) node match {
      case connector: Connector =>
        // Set it to zero to push all energy into other nodes, to
        // avoid energy loss when removing nodes. Set it back to the
        // original value though, as there are cases where the node
        // is re-used afterwards, without re-adjusting its buffer size.
        var bufferSize = connector.localBufferSize()
        connector.setLocalBufferSize(0)
        connector.setLocalBufferSize(bufferSize)
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  protected def result(args: Any*) = li.cil.oc.util.ResultWrapper.result(args: _*)

  // ----------------------------------------------------------------------- //

  override def getModelData: ModelData = ModelData.EMPTY

  @Deprecated
  override def hasProperty(prop: ModelProperty[_]) = false

  @Deprecated
  override def getData[T](prop: ModelProperty[T]): T = null.asInstanceOf[T]

  @Deprecated
  override def setData[T](prop: ModelProperty[T], value: T): T = null.asInstanceOf[T]
}
