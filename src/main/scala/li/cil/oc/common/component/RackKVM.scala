package li.cil.oc.common.component

import java.util
import java.util.UUID

import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.network.{Analyzable, Environment, EnvironmentHost, Message, Node, Visibility}
import li.cil.oc.api.util.{Lifecycle, StateAware}
import li.cil.oc.api.util.StateAware.State
import li.cil.oc.api.component.{RackBusConnectable, RackMountable}
import li.cil.oc.common.Tier
import li.cil.oc.common.datacomponents.{CompoundStorage, OCComponents, TerminalReference}
import li.cil.oc.util.ExtendedDataComponentHolder._
import li.cil.oc.{Constants, Settings, api}
import net.minecraft.core.Direction
import net.minecraft.core.component.{DataComponentHolder, DataComponentPatch}
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.common.MutableDataComponentHolder

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.mutable

class RackKVM(val rack: api.internal.Rack, val slot: Int)
  extends Environment with EnvironmentHost with Analyzable with RackMountable with Lifecycle with DeviceInfo with RemoteTerminalHost {

  override val node: Node = api.Network.newNode(this, Visibility.None).create()

  override val range: Double = Settings.get.maxWirelessRange(Tier.Two)
  val keys: mutable.ListBuffer[String] = mutable.ListBuffer.empty[String]

  private var selectedSlot = -1
  private var lastServerMask = -1
  private val attachedServers = Array.fill[Node](3)(null)

  val consoleSlots: Array[Int] = (0 until 4).filter(_ != slot).toArray

  lazy val buffers: Array[api.internal.TextBuffer] = Array.tabulate(3) { _ =>
    val screenItem = api.Items.get(Constants.BlockName.ScreenTier1).createItemStack(1)
    val result = api.Driver.driverFor(screenItem, getClass).createEnvironment(screenItem, this).asInstanceOf[api.internal.TextBuffer]
    val (maxWidth, maxHeight) = Settings.screenResolutionsByTier(Tier.Four)
    result.setMaximumResolution(maxWidth, maxHeight)
    result.setMaximumColorDepth(Settings.screenDepthsByTier(Tier.Four))
    result
  }

  lazy val keyboards: Array[api.internal.Keyboard] = Array.tabulate(3) { index =>
    val keyboardItem = api.Items.get(Constants.BlockName.Keyboard).createItemStack(1)
    val result = api.Driver.driverFor(keyboardItem, getClass).createEnvironment(keyboardItem, this).asInstanceOf[api.internal.Keyboard]
    result.setUsableOverride((_: api.internal.Keyboard, player: Player) =>
      selectedRackSlot == consoleSlots(index) && isRemoteUsable(player))
    result
  }

  override def buffer: api.internal.TextBuffer = buffers(consoleIndex(selectedRackSlot).getOrElse(0))

  override def sidedKeys: scala.collection.Seq[String] = {
    if (!rack.getEnvironmentLevel.isClientSide) keys
    else rack.getMountableData(slot).getComponent(OCComponents.KEYS).getOrElse(List.empty)
  }

  def selectedRackSlot: Int = {
    if (!rack.getEnvironmentLevel.isClientSide) selectedSlot
    else rack.getMountableData(slot).getComponent(OCComponents.RACK_KVM_SELECTED_SLOT).getOrElse(selectedSlot)
  }

  def serverMask: Int = {
    if (!rack.getEnvironmentLevel.isClientSide) computeServerMask()
    else rack.getMountableData(slot).getComponent(OCComponents.RACK_KVM_SERVER_MASK).getOrElse(0)
  }

  def bufferAtRackSlot(rackSlot: Int): Option[api.internal.TextBuffer] =
    consoleIndex(rackSlot).map(index => buffers(index))

  def selectRackSlot(rackSlot: Int): Boolean = {
    if (rack.getEnvironmentLevel.isClientSide || !isServerAt(rackSlot)) return false
    if (selectedSlot != rackSlot) {
      selectedSlot = rackSlot
      rack.markChanged(slot)
      rack.markChanged()
    }
    true
  }

  override def isBufferUsable(candidate: api.internal.TextBuffer, player: Player): Boolean =
    bufferAtRackSlot(selectedRackSlot).contains(candidate) && isRemoteUsable(player)

  private def consoleIndex(rackSlot: Int): Option[Int] = consoleSlots.indexOf(rackSlot) match {
    case index if index >= 0 => Some(index)
    case _ => None
  }

  private def isServerAt(rackSlot: Int): Boolean =
    rackSlot >= 0 && rackSlot < rack.getContainerSize && rack.getMountable(rackSlot).isInstanceOf[api.internal.Server]

  private def computeServerMask(): Int = consoleSlots.zipWithIndex.foldLeft(0) {
    case (mask, (rackSlot, index)) if isServerAt(rackSlot) => mask | (1 << index)
    case (mask, _) => mask
  }

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Rack KVM",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "RemoteViewing KVM"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  override def onConnect(connectedNode: Node): Unit = {
    if (connectedNode == node && !getEnvironmentLevel.isClientSide) reconcileConsoles()
  }

  override def onDisconnect(disconnectedNode: Node): Unit = {
    if (disconnectedNode == node) {
      buffers.foreach(_.node.remove())
      keyboards.foreach(_.node.remove())
      attachedServers.indices.foreach(attachedServers(_) = null)
    }
  }

  override def onMessage(message: Message): Unit = {}

  override def getEnvironmentLevel = rack.getEnvironmentLevel

  override def xPosition = rack.xPosition

  override def yPosition = rack.yPosition

  override def zPosition = rack.zPosition

  override def markChanged(): Unit = rack.markChanged()

  override def describeForClient(holder: MutableDataComponentHolder): Unit = {
    if (node.address == null) api.Network.joinNewNetwork(node)
    holder.setComponent(OCComponents.KEYS, keys.toList)
    holder.setComponent(OCComponents.ADDRESS, node.address)
    holder.setComponent(OCComponents.RACK_KVM_SELECTED_SLOT, selectedSlot)
    holder.setComponent(OCComponents.RACK_KVM_SERVER_MASK, computeServerMask())
  }

  override def getConnectableCount: Int = 0

  override def getConnectableAt(index: Int): RackBusConnectable = null

  override def onActivate(player: Player, hand: InteractionHand, heldItem: ItemStack, hitX: Float, hitY: Float): Boolean = {
    if (player.isCrouching && api.Items.get(heldItem) == api.Items.get(Constants.ItemName.Terminal)) {
      if (!getEnvironmentLevel.isClientSide) {
        val key = UUID.randomUUID().toString
        keys.clear()
        keys += key
        heldItem.setComponent(OCComponents.TERMINAL_REFERENCE, new TerminalReference(key, node.address))
        rack.markChanged(slot)
        rack.markChanged()
        player.getInventory.setChanged()
      }
      true
    }
    else false
  }

  override def loadData(holder: DataComponentHolder): Unit = {
    if (!getEnvironmentLevel.isClientSide) node.loadData(holder)

    for (index <- buffers.indices) {
      holder.getComponent(bufferComponent(index)).foreach(data => buffers(index).loadData(new CompoundStorage().andApply(data)))
      holder.getComponent(keyboardComponent(index)).foreach(data => keyboards(index).loadData(new CompoundStorage().andApply(data)))
    }

    keys.clear()
    keys ++= holder.getOrDefault(OCComponents.KEYS, List.empty)
    selectedSlot = holder.getOrDefault(OCComponents.RACK_KVM_SELECTED_SLOT, -1)
  }

  override def saveData(holder: MutableDataComponentHolder): Unit = {
    node.saveData(holder)
    for (index <- buffers.indices) {
      val bufferData = new CompoundStorage()
      buffers(index).saveData(bufferData)
      holder.setComponent(bufferComponent(index), bufferData.toPatch)

      val keyboardData = new CompoundStorage()
      keyboards(index).saveData(keyboardData)
      holder.setComponent(keyboardComponent(index), keyboardData.toPatch)
    }
    holder.setComponent(OCComponents.KEYS, keys.toList)
    holder.setComponent(OCComponents.RACK_KVM_SELECTED_SLOT, selectedSlot)
  }

  private def bufferComponent(index: Int): OCComponents.Type[DataComponentPatch] = index match {
    case 0 => OCComponents.RACK_KVM_BUFFER_0
    case 1 => OCComponents.RACK_KVM_BUFFER_1
    case 2 => OCComponents.RACK_KVM_BUFFER_2
  }

  private def keyboardComponent(index: Int): OCComponents.Type[DataComponentPatch] = index match {
    case 0 => OCComponents.RACK_KVM_KEYBOARD_0
    case 1 => OCComponents.RACK_KVM_KEYBOARD_1
    case 2 => OCComponents.RACK_KVM_KEYBOARD_2
  }

  override def canUpdate: Boolean = true

  override def update(): Unit = {
    if (!getEnvironmentLevel.isClientSide) reconcileConsoles()
    buffers.foreach(_.update())
  }

  private def reconcileConsoles(): Unit = {
    val mask = computeServerMask()
    if (mask != lastServerMask) {
      lastServerMask = mask
      rack.markChanged(slot)
    }

    if (selectedSlot < 0) {
      consoleSlots.find(isServerAt).foreach(found => selectedSlot = found)
      if (selectedSlot >= 0) rack.markChanged(slot)
    }

    for (index <- buffers.indices) {
      val screenNode = buffers(index).node
      val keyboardNode = keyboards(index).node
      if (screenNode.network == null) api.Network.joinNewNetwork(screenNode)
      screenNode.connect(keyboardNode)

      val target = rack.getMountable(consoleSlots(index)) match {
        case server: api.internal.Server => server.machine.node
        case _ => null
      }

      if (attachedServers(index) ne target) {
        if (attachedServers(index) != null) screenNode.disconnect(attachedServers(index))
        attachedServers(index) = target
        if (target != null) screenNode.connect(target)
      }
    }
  }

  override def getCurrentState: util.EnumSet[State] = util.EnumSet.noneOf(classOf[StateAware.State])

  override def onAnalyze(player: Player, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Array[Node] =
    buffers.map(_.node) ++ keyboards.map(_.node)

  override def onLifecycleStateChange(state: Lifecycle.LifecycleState): Unit = if (rack.getEnvironmentLevel.isClientSide) state match {
    case Lifecycle.LifecycleState.Initialized => TerminalServer.loaded.add(this)
    case Lifecycle.LifecycleState.Disposed => TerminalServer.loaded.remove(this)
    case _ =>
  }
}
