package li.cil.oc.server.component

import java.util
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.internal
import li.cil.oc.api.internal.Rotatable
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.common.item.data.NavigationUpgradeData
import li.cil.oc.common.Tier
import li.cil.oc.server.network.Waypoints
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.SableCompat
import net.minecraft.core.component.DataComponentHolder
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.{Direction, HolderLookup}
import net.minecraft.world.phys.Vec3

import scala.collection.convert.ImplicitConversionsToJava._
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.common.MutableDataComponentHolder

class UpgradeNavigation(val host: EnvironmentHost with Rotatable, val absolutePosition: Boolean = false) extends AbstractManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("navigation", Visibility.Neighbors).
    withConnector().
    create()

  val data = new NavigationUpgradeData()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> (if (absolutePosition) "Navigation card" else "Navigation upgrade"),
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "PathFinder v3",
    DeviceAttribute.Capacity -> (if (absolutePosition) "physical" else data.getSize(host.getEnvironmentLevel).toString)
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():number, number, number -- Get the current relative position of the robot.""")
  def getPosition(context: Context, args: Arguments): Array[AnyRef] = {
    val physical = SableCompat.physicalPosition(host)
    if (absolutePosition) result(physical.x, physical.y, physical.z)
    else {
      val info = data.mapData(host.getEnvironmentLevel)
      val size = data.getSize(host.getEnvironmentLevel)
      val relativeX = physical.x - info.centerX
      val relativeZ = physical.z - info.centerZ

      if (math.abs(relativeX) <= size / 2 && math.abs(relativeZ) <= size / 2)
        result(relativeX, physical.y, relativeZ)
      else
        result((), "out of range")
    }
  }

  @Callback(doc = """function():number -- Get the current orientation of the robot.""")
  def getFacing(context: Context, args: Arguments): Array[AnyRef] =
    result(SableCompat.physicalFacing(host.getEnvironmentLevel,
      new Vec3(host.xPosition, host.yPosition, host.zPosition), host.facing).ordinal)

  @Callback(doc = """function():number -- Get the current world-space heading in degrees (0=north, 90=east, 180=south, 270=west).""")
  def getHeading(context: Context, args: Arguments): Array[AnyRef] =
    result(SableCompat.physicalHeading(host.getEnvironmentLevel,
      new Vec3(host.xPosition, host.yPosition, host.zPosition), host.facing))

  @Callback(doc = """function():number -- Get the current world-space pitch in degrees above the horizontal plane.""")
  def getPitch(context: Context, args: Arguments): Array[AnyRef] =
    result(SableCompat.physicalPitch(host.getEnvironmentLevel,
      new Vec3(host.xPosition, host.yPosition, host.zPosition), host.facing))

  @Callback(doc = """function():number -- Get the operational range of the navigation upgrade.""")
  def getRange(context: Context, args: Arguments): Array[AnyRef] =
    result(if (absolutePosition) Double.PositiveInfinity else data.getSize(host.getEnvironmentLevel) / 2)

  @Callback(doc = """function(range:number):table -- Find waypoints in the specified range.""")
  def findWaypoints(context: Context, args: Arguments): Array[AnyRef] = {
    val range = args.checkDouble(0) max 0 min Settings.get.maxWirelessRange(Tier.Two)
    if (range <= 0) return result(Array.empty[AnyRef])
    if (!node.tryChangeBuffer(-range * Settings.get.wirelessCostPerRange(Tier.Two) * 0.25)) return result((), "not enough energy")
    context.pause(0.5)
    val position = BlockPosition(host)
    val positionVec = SableCompat.physicalPosition(host.getEnvironmentLevel, position.toVec3)
    val rangeSq = range * range
    val waypoints = Waypoints.findWaypoints(position, range).
      filter(waypoint => SableCompat.distanceSquared(host.getEnvironmentLevel, positionVec,
        SableCompat.physicalPosition(waypoint.getEnvironmentLevel,
          new net.minecraft.world.phys.Vec3(waypoint.x + 0.5, waypoint.y + 0.5, waypoint.z + 0.5))) <= rangeSq)
    result(waypoints.map(waypoint => {
      val waypointPosition = SableCompat.physicalPosition(waypoint.getEnvironmentLevel, waypoint.position.toVec3)
      val delta = waypointPosition.subtract(positionVec)
      Map(
        "position" -> Array(delta.x, delta.y, delta.z),
        "redstone" -> waypoint.maxInput,
        "label" -> waypoint.label,
        "address" -> waypoint.node.address()
      )
    }).toArray)
  }

  override def onMessage(message: Message): Unit = {
    super.onMessage(message)
    if (message.name == "tablet.use") message.source.host match {
      case machine: api.machine.Machine => (machine.host, message.data) match {
        case (tablet: internal.Tablet, Array(nbt: CompoundTag, stack: ItemStack, player: Player, blockPos: BlockPosition, side: Direction, hitX: java.lang.Float, hitY: java.lang.Float, hitZ: java.lang.Float)) =>
          if (!absolutePosition) {
            val info = data.mapData(host.getEnvironmentLevel)
            val physical = SableCompat.physicalPosition(blockPos.world.orNull, blockPos.toVec3)
            nbt.putInt("posX", math.floor(physical.x).toInt - info.centerX)
            nbt.putInt("posY", math.floor(physical.y).toInt)
            nbt.putInt("posZ", math.floor(physical.z).toInt - info.centerZ)
          }
        case _ => // Ignore.
      }
      case _ => // Ignore.
    }
  }

  // ----------------------------------------------------------------------- //

  override def loadData(holder: DataComponentHolder): Unit = {
    super.loadData(holder)
    data.loadData(holder)
  }

  override def saveData(holder: MutableDataComponentHolder): Unit = {
    super.saveData(holder)
    data.saveData(holder)
  }
}
