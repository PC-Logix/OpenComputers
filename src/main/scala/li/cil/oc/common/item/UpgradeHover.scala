package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.neoforged.neoforge.common.extensions.IItemExtension


class UpgradeHover(props: Properties, val tier: Int) extends Item(props) with traits.SimpleItem with traits.ItemTier with IItemExtension {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override protected def tooltipData = Seq(Settings.get.upgradeFlightHeight(tier))
}
