package li.cil.oc.common.item

import li.cil.oc.Settings
import li.cil.oc.Settings.DebugCardAccess
import li.cil.oc.common.item.data.DebugCardData
import li.cil.oc.server.component.{DebugCard => CDebugCard}
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.{Item, ItemStack, TooltipFlag}
import net.minecraft.world.level.Level
import net.minecraft.world.{InteractionHand, InteractionResult, InteractionResultHolder}
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.common.extensions.IItemExtension

import java.util

class DebugCard(props: Properties) extends Item(props) with traits.ComponentItem with IItemExtension {

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[Component], flag: TooltipFlag): Unit = {
    super.tooltipExtended(stack, tooltip, flag)
    val data = new DebugCardData(stack)
    data.access.foreach(access => tooltip.add(Component.literal(s"§8${access.player}§r")))
  }

  override def use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder[ItemStack] = {
    val stack = player.getItemInHand(hand)
    if (!level.isClientSide && player.isCrouching) {
      val data = new DebugCardData(stack)
      val name = player.getName

      if (data.access.exists(_.player == name.getString)) data.access = None
      else data.access =
        Some(CDebugCard.AccessContext(name.getString, Settings.get.debugCardAccess match {
          case wl: DebugCardAccess.Whitelist => wl.nonce(name.getString) match {
            case Some(n) => n
            case None =>
              player.sendSystemMessage(Component.literal("§cYou are not whitelisted to use debug card"))
              return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
          }

          case _ => ""
        }))

      data.saveData(stack)
    }

    InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
  }
}
