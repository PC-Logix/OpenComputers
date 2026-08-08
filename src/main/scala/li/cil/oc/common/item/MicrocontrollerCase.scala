package li.cil.oc.common.item

import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.common.extensions.IItemExtension


class MicrocontrollerCase(props: Properties, val tier: Int) extends Item(props) with traits.SimpleItem with traits.ItemTier with IItemExtension {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override protected def tierFromDriver(stack: ItemStack) = tier
}
