package li.cil.oc.common.item

import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.common.extensions.IItemExtension


class Microchip(props: Properties, val tier: Int) extends Item(props) with traits.SimpleItem with IItemExtension {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier
}
