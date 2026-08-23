package li.cil.oc.integration.opencomputers

import li.cil.oc.{api, Settings}
import li.cil.oc.api.driver.DriverItem
import li.cil.oc.api.internal
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Tier
import li.cil.oc.server.driver.Registry
import li.cil.oc.util.ItemUtils
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

import java.util.function.Consumer
import scala.annotation.tailrec

trait Item extends DriverItem {
  def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]): Boolean =
    worksWith(stack) && !Registry.blacklist.exists {
      case (blacklistedStack, blacklistedHost) =>
        ItemStack.isSameItem(stack, blacklistedStack) &&
          blacklistedHost.exists(_.isAssignableFrom(host))
    }

  override def tier(stack: ItemStack) = Tier.One

  protected def isOneOf(stack: ItemStack, items: api.detail.ItemInfo*): Boolean = items.filter(_ != null).contains(api.Items.get(stack))

  protected def isAdapter(host: Class[_ <: EnvironmentHost]): Boolean = classOf[internal.Adapter].isAssignableFrom(host)

  protected def isComputer(host: Class[_ <: EnvironmentHost]): Boolean = classOf[internal.Case].isAssignableFrom(host)

  protected def isRobot(host: Class[_ <: EnvironmentHost]): Boolean = classOf[internal.Robot].isAssignableFrom(host)

  protected def isRotatable(host: Class[_ <: EnvironmentHost]): Boolean = classOf[internal.Rotatable].isAssignableFrom(host)

  protected def isServer(host: Class[_ <: EnvironmentHost]): Boolean = classOf[internal.Server].isAssignableFrom(host)

  protected def isTablet(host: Class[_ <: EnvironmentHost]): Boolean = classOf[internal.Tablet].isAssignableFrom(host)

  protected def isMicrocontroller(host: Class[_ <: EnvironmentHost]): Boolean = classOf[internal.Microcontroller].isAssignableFrom(host)

  protected def isDrone(host: Class[_ <: EnvironmentHost]): Boolean = classOf[internal.Drone].isAssignableFrom(host)
}

object Item {
  def dataTag(stack: ItemStack): CompoundTag = {
    val nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
    if (!nbt.contains(Settings.namespace + "data")) {
      nbt.put(Settings.namespace + "data", new CompoundTag())
    }
    nbt.getCompound(Settings.namespace + "data")
  }

  def updateDataTag(stack: ItemStack, fn: Consumer[CompoundTag]): Unit = {
    CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt => {
      if (!nbt.contains(Settings.namespace + "data")) {
        nbt.put(Settings.namespace + "data", new CompoundTag())
      }

      fn.accept(nbt.getCompound(Settings.namespace + "data"))
    })
  }

  @tailrec
  private def getTag(tagCompound: CompoundTag, keys: Array[String]): Option[CompoundTag] = {
    if (keys.length == 0) Option(tagCompound)
    else if (!tagCompound.contains(keys(0))) None
    else getTag(tagCompound.getCompound(keys(0)), keys.drop(1))
  }

  private def getTag(stack: ItemStack, keys: Array[String]): Option[CompoundTag] = {
    val tag = ItemUtils.getTag(stack)
    if (stack == null || stack.getCount == 0 || stack == ItemStack.EMPTY) None
    else if (tag == null) None
    else getTag(tag, keys)
  }

  def address(stack: ItemStack): Option[String] = {
    val addressKey = "address"
    getTag(stack, Array(Settings.namespace + "data", "node")) match {
      case Some(tag) if tag.contains(addressKey) => Option(tag.getString(addressKey))
      case _ => None
    }
  }
}
