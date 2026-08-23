package li.cil.oc.common.item

import li.cil.oc.Localization
import li.cil.oc.common.datacomponents.{MFCoords, OCComponents}
import li.cil.oc.util.Tooltip
import net.minecraft.network.chat.Component
import net.minecraft.world.item.{Item, ItemStack, TooltipFlag}
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.UseOnContext
import net.neoforged.neoforge.common.extensions.IItemExtension

import java.util

class UpgradeMF(props: Properties) extends Item(props) with traits.SimpleItem with traits.ItemTier with IItemExtension {
  override def onItemUseFirst(stack: ItemStack, ctx: UseOnContext): InteractionResult = {
    val level = ctx.getLevel
    if (!level.isClientSide && ctx.isSecondaryUseActive) {
      stack.set(OCComponents.MF_COORD, MFCoords(level.dimension.location, ctx.getClickedPos, ctx.getClickedFace))
      return InteractionResult.sidedSuccess(level.isClientSide)
    }

    super.onItemUseFirst(stack, ctx)
  }

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[Component], flag: TooltipFlag): Unit = {
    tooltip.add(Component.literal(Localization.Tooltip.MFULinked(stack.has(OCComponents.MF_COORD))).setStyle(Tooltip.DefaultStyle))
  }
}
