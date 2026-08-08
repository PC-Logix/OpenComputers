package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.common.extensions.IItemExtension

class Memory(props: Properties, val tier: Int) extends Item(props) with traits.SimpleItem with traits.ItemTier with IItemExtension {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  val kiloBytes: Int = Settings.get.ramSizes(tier)

  override def getName(stack: ItemStack): Component = {
    // Always show size of memory in KB because it matches the OpenOS boot
    // banner
    super.getName(stack).copy().append(s" (${kiloBytes}KB)")
  }
}
