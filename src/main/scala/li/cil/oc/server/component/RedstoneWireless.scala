package li.cil.oc.server.component

import li.cil.oc.{Constants, Settings}
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network._
import li.cil.oc.common.EventHandler
import li.cil.oc.common.datacomponents.{OCComponents, WirelessRedstoneState}
import li.cil.oc.integration.util
import li.cil.oc.util.ExtendedDataComponentHolder._
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentHolder
import net.minecraft.nbt.CompoundTag
import net.neoforged.neoforge.common.MutableDataComponentHolder

import scala.collection.convert.ImplicitConversionsToJava._

trait RedstoneWireless extends RedstoneSignaller with DeviceInfo {
  def redstone: EnvironmentHost

  var wirelessFrequency = 0

  var wirelessInput = false

  var wirelessOutput = false

  // ----------------------------------------------------------------------- //

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Communication,
    DeviceAttribute.Description -> "Wireless redstone controller",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Rw400-M",
    DeviceAttribute.Capacity -> "1",
    DeviceAttribute.Width -> "1"
  )

  override def getDeviceInfo: java.util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():number -- Get the wireless redstone input.""")
  def getWirelessInput(context: Context, args: Arguments): Array[AnyRef] = {
    wirelessInput = util.WirelessRedstone.getInput(this)
    result(wirelessInput)
  }

  @Callback(direct = true, doc = """function():boolean -- Get the wireless redstone output.""")
  def getWirelessOutput(context: Context, args: Arguments): Array[AnyRef] = result(wirelessOutput)

  @Callback(doc = """function(value:boolean):boolean -- Set the wireless redstone output.""")
  def setWirelessOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val oldValue = wirelessOutput
    val newValue = args.checkBoolean(0)

    if (oldValue != newValue) {
      wirelessOutput = newValue

      util.WirelessRedstone.updateOutput(this)

      if (Settings.get.redstoneDelay > 0)
        context.pause(Settings.get.redstoneDelay)
    }

    result(oldValue)
  }

  @Callback(direct = true, doc = """function():number -- Get the currently set wireless redstone frequency.""")
  def getWirelessFrequency(context: Context, args: Arguments): Array[AnyRef] = result(wirelessFrequency)

  @Callback(doc = """function(frequency:number):number -- Set the wireless redstone frequency to use.""")
  def setWirelessFrequency(context: Context, args: Arguments): Array[AnyRef] = {
    val oldValue = wirelessFrequency
    val newValue = args.checkInteger(0)

    if (oldValue != newValue) {
      util.WirelessRedstone.removeReceiver(this)
      util.WirelessRedstone.removeTransmitter(this)

      wirelessFrequency = newValue
      wirelessInput = false
      wirelessOutput = false

      util.WirelessRedstone.addReceiver(this)

      context.pause(0.5)
    }

    result(oldValue)
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node): Unit = {
    super.onConnect(node)
    if (node == this.node) {
      EventHandler.scheduleWirelessRedstone(this)
    }
  }

  override def onDisconnect(node: Node): Unit = {
    super.onDisconnect(node)
    if (node == this.node) {
      util.WirelessRedstone.removeReceiver(this)
      util.WirelessRedstone.removeTransmitter(this)
      wirelessOutput = false
      wirelessFrequency = 0
    }
  }

  // ----------------------------------------------------------------------- //

  override def loadData(holder: DataComponentHolder): Unit = {
    super.loadData(holder)

    for(state <- holder.getComponent(OCComponents.WIRELESS_REDSTONE_STATE)) {
      wirelessFrequency = state.frequency()
      wirelessInput = state.input()
      wirelessOutput = state.output()
    }
  }

  override def saveData(holder: MutableDataComponentHolder): Unit = {
    super.saveData(holder)
    holder.setComponent(OCComponents.WIRELESS_REDSTONE_STATE, new WirelessRedstoneState(
      wirelessFrequency,
      wirelessInput,
      wirelessOutput
    ))
  }
}
