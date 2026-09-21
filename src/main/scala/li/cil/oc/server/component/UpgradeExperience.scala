package li.cil.oc.server.component

import java.util
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.util.{UpgradeExperience => ExperienceUtil}
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.Items

class UpgradeExperience(val host: EnvironmentHost with internal.Agent) extends AbstractManagedEnvironment with DeviceInfo {
  final val MaxLevel = 30

  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("experience").
    withConnector(30 * Settings.get.bufferPerLevel).
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Knowledge database",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "ERSO (Event Recorder and Self-Optimizer)",
    DeviceAttribute.Capacity -> "30"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  var experience = 0.0

  var level = 0

  // The node starts with its maximum connector size so persisted charge can
  // be loaded without being discarded. Normalize the fresh component before
  // it can be connected, otherwise a zero-XP upgrade temporarily grants the
  // level-30 energy capacity until its first XP update.
  updateXpInfo()

  def xpForNextLevel: Double = ExperienceUtil.xpForLevel(level + 1)

  def addExperience(value: Double): Unit = {
    if (level < MaxLevel) {
      experience = experience + value
      if (experience >= xpForNextLevel) {
        updateXpInfo()
      }
      val world = this.host.getEnvironmentLevel
      val pos = this.host.player.blockPosition
      val orb = new ExperienceOrb(world, pos.getX.toDouble + 0.5D, pos.getY.toDouble + 0.5D, pos.getZ.toDouble + 0.5D, value.toInt)
      this.host.player.takeXpDelay = 0
      orb.playerTouch(this.host.player)
    }
  }

  def updateXpInfo(): Unit = {
    // xp(level) = base + (level * const) ^ exp
    // pow(xp(level) - base, 1/exp) / const = level
    val oldLevel = level
    level = ExperienceUtil.calculateLevelFromExperience(experience)
    if (node != null) {
      if (level != oldLevel) {
        updateClient()
      }
      node.setLocalBufferSize(Settings.get.bufferPerLevel * level)
    }
  }

  @Callback(direct = true, doc = """function():number -- The current level of experience stored in this experience upgrade.""")
  def level(context: Context, args: Arguments): Array[AnyRef] =
    result(ExperienceUtil.calculateExperienceLevel(level, experience))

  @Callback(doc = """function():boolean -- Tries to consume an enchanted item to add experience to the upgrade.""")
  def consume(context: Context, args: Arguments): Array[AnyRef] = {
    if (level >= MaxLevel) {
      return result((), "max level")
    }
    val stack = host.mainInventory.getItem(host.selectedSlot)
    if (stack.isEmpty) {
      return result((), "no item")
    }
    var xp = 0
    if (stack.getItem == Items.EXPERIENCE_BOTTLE) {
      xp += 3 + host.getEnvironmentLevel.random.nextInt(5) + host.getEnvironmentLevel.random.nextInt(5)
    }
    else {
      for (entry <- EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
        val enchantment = entry.getKey.value()
        val level = entry.getIntValue
        if (enchantment != null) {
          xp += enchantment.getMinCost(level)
        }
      }
      if (xp <= 0) {
        return result((), "could not extract experience from item")
      }
    }
    val consumed = host.mainInventory().removeItem(host.selectedSlot, 1)
    if (consumed.isEmpty) {
      return result((), "could not consume item")
    }
    addExperience(xp * Settings.get.constantXpGrowth)
    result(true)
  }

  private def updateClient() = host match {
    case robot: internal.Robot => robot.synchronizeSlot(robot.componentSlot(node.address))
    case _ =>
  }

  override def saveData(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.saveData(nbt, provider)
    ExperienceUtil.setExperience(nbt, experience)
  }

  override def loadData(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadData(nbt, provider)
    experience = ExperienceUtil.getExperience(nbt)
    updateXpInfo()
  }
}
