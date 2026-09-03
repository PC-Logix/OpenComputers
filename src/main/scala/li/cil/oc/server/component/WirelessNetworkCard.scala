package li.cil.oc.server.component

import java.io._
import java.util
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.SableCompat
import li.cil.oc.util.ExtendedLevel._
import li.cil.oc.util.ExtendedDataComponentHolder._
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentHolder
import net.minecraft.nbt.CompoundTag
import net.neoforged.neoforge.common.MutableDataComponentHolder

import scala.collection.convert.ImplicitConversionsToJava._
import scala.language.implicitConversions

abstract class WirelessNetworkCard(host: EnvironmentHost) extends NetworkCard(host) with WirelessEndpoint {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("modem", Visibility.Neighbors).
    withConnector().
    create()

  protected def wirelessCostPerRange: Double

  protected def maxWirelessRange: Double

  protected def shouldSendWiredTraffic: Boolean

  var strength = maxWirelessRange

  def position = BlockPosition(host)

  override def x = position.x

  override def y = position.y

  override def z = position.z

  override def getWirelessLevel = host.getEnvironmentLevel

  def receivePacket(packet: Packet, source: WirelessEndpoint): Unit = {
    val sourcePosition = SableCompat.physicalPosition(getWirelessLevel,
      new net.minecraft.world.phys.Vec3(source.x + 0.5, source.y + 0.5, source.z + 0.5))
    val receiverPosition = SableCompat.physicalPosition(getWirelessLevel,
      new net.minecraft.world.phys.Vec3(host.xPosition, host.yPosition, host.zPosition))
    val distance = Math.sqrt(SableCompat.distanceSquared(getWirelessLevel, sourcePosition, receiverPosition))
    receivePacket(packet, distance, host)
  }

  // ----------------------------------------------------------------------- //
  
  @Callback(direct = true, doc = """function():number -- Get the signal strength (range) used when sending messages.""")
  def getStrength(context: Context, args: Arguments): Array[AnyRef] = result(strength)

  @Callback(doc = """function(strength:number):number -- Set the signal strength (range) used when sending messages.""")
  def setStrength(context: Context, args: Arguments): Array[AnyRef] = {
    strength = math.max(0, math.min(args.checkDouble(0), maxWirelessRange))
    result(strength)
  }

  override def isWireless(context: Context, args: Arguments): Array[AnyRef] = result(true)
  
  override def isWired(context: Context, args: Arguments): Array[AnyRef] = result(shouldSendWiredTraffic)
  
  override protected def doSend(packet: Packet): Unit = {
    if (strength > 0) {
      checkPower()
      api.Network.sendWirelessPacket(this, strength, packet)
    }
    if (shouldSendWiredTraffic)
      super.doSend(packet)
  }

  override protected def doBroadcast(packet: Packet): Unit = {
    if (strength > 0) {
      checkPower()
      api.Network.sendWirelessPacket(this, strength, packet)
    }
    if (shouldSendWiredTraffic)
      super.doBroadcast(packet)
  }
  
  private def checkPower(): Unit = {
    val cost = wirelessCostPerRange
    if (cost > 0 && !Settings.get.ignorePower) {
      if (!node.asInstanceOf[Connector].tryChangeBuffer(-strength * cost)) {
        throw new IOException("not enough energy")
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update(): Unit = {
    super.update()
    if (getWirelessLevel.getGameTime % 20 == 0) {
      api.Network.updateWirelessNetwork(this)
    }
  }

  override def onConnect(node: Node): Unit = {
    super.onConnect(node)
    if (node == this.node) {
      api.Network.joinWirelessNetwork(this)
    }
  }

  override def onDisconnect(node: Node): Unit = {
    super.onDisconnect(node)
    if (node == this.node || !getWirelessLevel.isLoaded(position)) {
      api.Network.leaveWirelessNetwork(this)
    }
  }

  // ----------------------------------------------------------------------- //

  override def loadData(holder: DataComponentHolder): Unit = {
    super.loadData(holder)
    for(strength <- holder.getComponent(OCComponents.STRENGTH)) {
      this.strength = strength
    }
  }

  override def saveData(holder: MutableDataComponentHolder): Unit = {
    super.saveData(holder)
    holder.setComponent(OCComponents.STRENGTH, strength)
  }
}

object WirelessNetworkCard {
  class Tier1(host: EnvironmentHost) extends WirelessNetworkCard(host) {
    override protected def wirelessCostPerRange: Double = Settings.get.wirelessCostPerRange(Tier.One)
    
    override protected def maxWirelessRange: Double = Settings.get.maxWirelessRange(Tier.One)
    
    // wired network card is before wireless cards in max port list
    override protected def maxOpenPorts: Int = Settings.get.maxOpenPorts(Tier.One + 1)
    
    override protected def shouldSendWiredTraffic = false

    // ----------------------------------------------------------------------- //

    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Network,
      DeviceAttribute.Description -> "Wireless ethernet controller",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "39i110 (LPPW-01)",
      DeviceAttribute.Version -> "1.0",
      DeviceAttribute.Capacity -> Settings.get.maxNetworkPacketSize.toString,
      DeviceAttribute.Size -> maxOpenPorts.toString,
      DeviceAttribute.Width -> maxWirelessRange.toString
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo

    override protected def isPacketAccepted(packet: Packet, distance: Double): Boolean = {
      if (distance <= maxWirelessRange && (distance > 0 || shouldSendWiredTraffic)) {
        super.isPacketAccepted(packet, distance)
      } else {
        false
      }
    }
  }

  class Tier2(host: EnvironmentHost) extends Tier1(host) {
    override protected def wirelessCostPerRange: Double = Settings.get.wirelessCostPerRange(Tier.Two)
    
    override protected def maxWirelessRange: Double = Settings.get.maxWirelessRange(Tier.Two)
    
    // wired network card is before wireless cards in max port list
    override protected def maxOpenPorts: Int = Settings.get.maxOpenPorts(Tier.Two + 1)
    
    override protected def shouldSendWiredTraffic = true

    // ----------------------------------------------------------------------- //

    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Network,
      DeviceAttribute.Description -> "Wireless ethernet controller",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "62i230 (MPW-01)",
      DeviceAttribute.Version -> "2.0",
      DeviceAttribute.Capacity -> Settings.get.maxNetworkPacketSize.toString,
      DeviceAttribute.Size -> maxOpenPorts.toString,
      DeviceAttribute.Width -> maxWirelessRange.toString
    )
    
    override def getDeviceInfo: util.Map[String, String] = deviceInfo
  }
}
