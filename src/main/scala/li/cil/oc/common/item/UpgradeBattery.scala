package li.cil.oc.common.item

import li.cil.oc.Settings
import li.cil.oc.api.driver.item.Chargeable
import li.cil.oc.common.item.data.NodeData
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.neoforged.neoforge.common.extensions.IItemExtension


class UpgradeBattery(props: Properties, val tier: Int) extends Item(props) with traits.SimpleItem with traits.ItemTier with traits.Chargeable with IItemExtension {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override protected def tooltipData = Seq(Settings.get.bufferCapacitorUpgrades(tier).toInt)

  override def isBarVisible(stack: ItemStack) = true

  override def getBarWidth(stack: ItemStack): Int = {
    val data = new NodeData(stack)
    val ratio = data.buffer.getOrElse(0.0) / Settings.get.bufferCapacitorUpgrades(tier)
    Math.round(ratio * 13.0f).toInt
  }

  // ----------------------------------------------------------------------- //

  override def canCharge(stack: ItemStack): Boolean = true

  override def charge(stack: ItemStack, amount: Double, simulate: Boolean): Double = {
    val data = new NodeData(stack)
    val buffer = data.buffer match {
      case Some(value) => value
      case _ => 0.0
    }
    traits.Chargeable.applyCharge(amount, buffer, Settings.get.bufferCapacitorUpgrades(tier), used => if (!simulate) {
      data.buffer = Option(buffer + used)
      data.saveData(stack)
    })
  }

  override def maxCharge(stack: ItemStack): Double = Settings.get.bufferCapacitorUpgrades(tier)

  override def getCharge(stack: ItemStack): Double = new NodeData(stack).buffer.getOrElse(0.0)

  override def setCharge(stack: ItemStack, amount: Double): Unit = {
    val data = new NodeData(stack)
    data.buffer = Option((0.0 max amount) min maxCharge(stack))
    data.saveData(stack)
  }

  override def canExtract(stack: ItemStack): Boolean = true
}
