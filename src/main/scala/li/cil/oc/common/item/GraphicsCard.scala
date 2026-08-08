package li.cil.oc.common.item

import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.neoforged.neoforge.common.extensions.IItemExtension


class GraphicsCard(props: Properties, val tier: Int) extends Item(props) with traits.SimpleItem with traits.ItemTier with traits.GPULike with IItemExtension {
  override protected def canResetComponentIdentity = true

  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override def gpuTier = tier
}