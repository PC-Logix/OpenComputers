package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.SableCompat
import net.minecraft.core.Direction

import scala.collection.convert.ImplicitConversionsToJava._
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome.Precipitation

class UpgradeSolarGenerator(val host: EnvironmentHost) extends AbstractManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withConnector().
    create()

  var ticksUntilCheck = 0

  var isSunShining = false

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Power,
    DeviceAttribute.Description -> "Solar panel",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Enligh10"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update(): Unit = {
    super.update()

    ticksUntilCheck -= 1
    if (ticksUntilCheck <= 0) {
      ticksUntilCheck = 100
      isSunShining = isSunVisible
    }
    if (isSunShining) {
      node.changeBuffer(Settings.get.solarGeneratorEfficiency)
    }
  }

  private def isSunVisible = {
    val blockPos = BlockPosition(host).offset(Direction.UP)
    val physical = SableCompat.physicalPosition(host.getEnvironmentLevel, blockPos.toVec3)
    val physicalBlockPos = net.minecraft.core.BlockPos.containing(physical)
    host.getEnvironmentLevel.isDay &&
      (host.getEnvironmentLevel.dimension != Level.NETHER) &&
      host.getEnvironmentLevel.canSeeSkyFromBelowWater(physicalBlockPos) &&
      (host.getEnvironmentLevel.getBiome(physicalBlockPos).value.getPrecipitationAt(physicalBlockPos) == Precipitation.NONE || (!host.getEnvironmentLevel.isRaining && !host.getEnvironmentLevel.isThundering))
  }
}
