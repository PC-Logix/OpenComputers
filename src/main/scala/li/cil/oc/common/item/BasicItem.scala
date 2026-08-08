package li.cil.oc.common.item

import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.neoforged.neoforge.common.extensions.IItemExtension


final class BasicItem(props: Properties, name: String) extends Item(props) with traits.SimpleItem with IItemExtension {
  unlocalizedName = name
}

final class BasicTieredItem(props: Properties, name: String) extends Item(props) with traits.SimpleItem with traits.ItemTier with IItemExtension {
  unlocalizedName = name
}
