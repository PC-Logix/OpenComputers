package li.cil.oc.common.item

import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.neoforged.neoforge.common.extensions.IItemExtension

import scala.language.existentials

class CPU(props: Properties, val tier: Int) extends Item(props) with traits.SimpleItem with traits.ItemTier with traits.CPULike with IItemExtension {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override def cpuTier = tier

  override protected def tooltipName = Option(unlocalizedName)

  override protected def canResetComponentIdentity: Boolean = true
}
