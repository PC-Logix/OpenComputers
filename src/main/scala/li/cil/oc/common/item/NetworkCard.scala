package li.cil.oc.common.item

import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.neoforged.neoforge.common.extensions.IItemExtension


class NetworkCard(props: Properties) extends Item(props) with traits.SimpleItem with traits.ItemTier with IItemExtension {
  override protected def canResetComponentIdentity: Boolean = true
}
