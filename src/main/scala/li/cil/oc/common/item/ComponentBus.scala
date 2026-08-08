package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.common.extensions.IItemExtension


class ComponentBus(props: Properties, val tier: Int) extends Item(props) with traits.SimpleItem with traits.ItemTier with IItemExtension {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override protected def tierFromDriver(stack: ItemStack) = tier

  override protected def tooltipData = Seq(Settings.get.cpuComponentSupport(tier))
}
