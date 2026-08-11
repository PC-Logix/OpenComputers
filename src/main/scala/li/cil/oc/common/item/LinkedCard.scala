package li.cil.oc.common.item

import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.util.ExtendedDataComponentHolder.convert
import li.cil.oc.util.Tooltip
import net.minecraft.network.chat.Component
import net.minecraft.world.item.{Item, ItemStack, TooltipFlag}
import net.minecraft.world.item.Item.{Properties, TooltipContext}
import net.neoforged.neoforge.common.extensions.IItemExtension

import java.util
import scala.jdk.CollectionConverters._

class LinkedCard(props: Properties) extends Item(props) with traits.SimpleItem with traits.ItemTier with IItemExtension {
  override def appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: util.List[Component], flag: TooltipFlag): Unit = {
    super.appendHoverText(stack, context, tooltip, flag)

    stack.getComponent(OCComponents.TUNNEL).foreach(channel => {
      val truncatedChannel = if (channel.length > 13) {
        channel.substring(0, 13) + "..."
      } else {
        channel
      }

      for (curr <- Tooltip.get(unlocalizedName + "_channel", truncatedChannel).asScala) {
        tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle))
      }
    })
  }
}
