package li.cil.oc.common.blockentity.traits

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.common.blockentity.traits
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.MovingAverage
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.{Direction, HolderLookup}
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

import scala.collection.mutable
import net.minecraft.nbt.Tag

import java.util.function.Consumer
import scala.jdk.CollectionConverters._

trait Hub extends traits.Environment with SidedEnvironment with Tickable {
  override def node: Node = null

  override protected def isConnected = plugs.exists(plug =>
    plug != null &&
    plug.node != null &&
    plug.node.address != null &&
    plug.node.network != null)

  protected val plugs = Direction.values.map(side => createPlug(side))

  val queue = mutable.Queue.empty[(Option[Direction], Packet)]

  var maxQueueSize = queueBaseSize

  var relayDelay = relayBaseDelay

  var relayAmount = relayBaseAmount

  var relayCooldown = -1

  // 20 cycles
  val packetsPerCycleAvg = new MovingAverage(20)

  // ----------------------------------------------------------------------- //

  protected def queueBaseSize = Settings.get.switchDefaultMaxQueueSize

  protected def queueSizePerUpgrade = Settings.get.switchQueueSizeUpgrade

  protected def relayBaseDelay = Settings.get.switchDefaultRelayDelay

  protected def relayDelayPerUpgrade = Settings.get.switchRelayDelayUpgrade

  protected def relayBaseAmount = Settings.get.switchDefaultRelayAmount

  protected def relayAmountPerUpgrade = Settings.get.switchRelayAmountUpgrade

  // ----------------------------------------------------------------------- //

  @OnlyIn(Dist.CLIENT)
  override def canConnect(side: Direction) = side != null

  override def sidedNode(side: Direction) = if (side != null) plugs(side.ordinal).node else null

  // ----------------------------------------------------------------------- //

  override def updateEntity(): Unit = {
    super.updateEntity()
    if (relayCooldown > 0) {
      relayCooldown -= 1
    }
    else {
      relayCooldown = -1
      if (queue.nonEmpty) queue.synchronized {
        val packetsToRely = math.min(queue.size, relayAmount)
        packetsPerCycleAvg.add(packetsToRely)
        for (i <- 0 until packetsToRely) {
          val (sourceSide, packet) = queue.dequeue()
          relayPacket(sourceSide, packet)
        }
        if (queue.nonEmpty) {
          relayCooldown = relayDelay - 1
        }
      }
      else if (getLevel.getGameTime % relayDelay == 0) {
        packetsPerCycleAvg.add(0)
      }
    }
  }

  def tryEnqueuePacket(sourceSide: Option[Direction], packet: Packet) = queue.synchronized {
    if (packet.ttl > 0 && queue.size < maxQueueSize) {
      queue += sourceSide -> packet.hop()
      if (relayCooldown < 0) {
        relayCooldown = relayDelay - 1
      }
      true
    }
    else false
  }

  protected def relayPacket(sourceSide: Option[Direction], packet: Packet): Unit = {
    for (side <- Direction.values) {
      if (sourceSide.isEmpty || sourceSide.get != side) {
        val node = sidedNode(side)
        if (node != null) {
          node.sendToReachable("network.message", packet)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  private final val PlugsTag = Settings.namespace + "plugs"
  private final val QueueTag = Settings.namespace + "queue"
  private final val SideTag = "side"
  private final val RelayCooldownTag = Settings.namespace + "relayCooldown"

  override def loadForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadForServer(nbt, provider)
    nbt.getList(PlugsTag, 10).asScala.zipWithIndex.foreach {
      case (tag, index) =>
        plugs(index).node.loadData(tag.asInstanceOf[CompoundTag], provider)
    }
    nbt.getList(QueueTag, 10).forEach((t: Tag) => {
      val tag = t.asInstanceOf[CompoundTag]
      val side = tag.getDirection(SideTag)
      val packet = api.Network.newPacket(tag)
      queue += side -> packet
    })
    if (nbt.contains(RelayCooldownTag)) {
      relayCooldown = nbt.getInt(RelayCooldownTag)
    }
  }

  override def saveForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = queue.synchronized {
    super.saveForServer(nbt, provider)
    // Side check for Waila (and other mods that may call this client side).
    if (isServer) {
      nbt.setNewTagList(PlugsTag, plugs.map(plug => {
        val plugNbt = new CompoundTag()
        if (plug.node != null)
          plug.node.saveData(plugNbt, provider)
        plugNbt
      }))
      nbt.setNewTagList(QueueTag, queue.map {
        case (sourceSide, packet) =>
          val tag = new CompoundTag()
          tag.setDirection(SideTag, sourceSide)
          packet.saveData(tag)
          tag
      })
      if (relayCooldown > 0) {
        nbt.putInt(RelayCooldownTag, relayCooldown)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  protected def createPlug(side: Direction) = new Plug(side)

  protected class Plug(val side: Direction) extends api.network.Environment {
    val node = createNode(this)

    override def onMessage(message: Message): Unit = {
      if (isPrimary) {
        onPlugMessage(this, message)
      }
    }

    override def onConnect(node: Node) = onPlugConnect(this, node)

    override def onDisconnect(node: Node) = onPlugDisconnect(this, node)

    def isPrimary = plugs(plugs.indexWhere(_.node.network == node.network)) == this

    def plugsInOtherNetworks = plugs.filter(_.node.network != node.network)
  }

  protected def onPlugConnect(plug: Plug, node: Node): Unit = {}

  protected def onPlugDisconnect(plug: Plug, node: Node): Unit = {}

  protected def onPlugMessage(plug: Plug, message: Message): Unit = {
    if (message.name == "network.message" && !plugs.exists(_.node == message.source)) message.data match {
      case Array(packet: Packet) => tryEnqueuePacket(Option(plug.side), packet)
      case _ =>
    }
  }

  protected def createNode(plug: Plug): Node = api.Network.newNode(plug, Visibility.Network).create()
}
