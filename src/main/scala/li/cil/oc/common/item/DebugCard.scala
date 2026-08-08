package li.cil.oc.common.item

import java.util
import li.cil.oc.Settings
import li.cil.oc.Settings.DebugCardAccess
import li.cil.oc.common.item.data.DebugCardData
import li.cil.oc.server.component.{DebugCard => CDebugCard}
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.Util
import net.neoforged.neoforge.common.extensions.IItemExtension

class DebugCard(props: Properties) extends Item(props) with traits.SimpleItem with IItemExtension {
  override protected def canResetComponentIdentity: Boolean = true

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[Component]): Unit = {
    super.tooltipExtended(stack, tooltip)
    val data = new DebugCardData(stack)
    data.access.foreach(access => tooltip.add(Component.literal(s"§8${access.player}§r")))
  }

  override def use(stack: ItemStack, level: Level, player: Player): InteractionResultHolder[ItemStack] = {
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
              player.swing(InteractionHand.MAIN_HAND)
              return new InteractionResultHolder[ItemStack](InteractionResult.FAIL, stack)
          }

          case _ => ""
        }))

      data.saveData(stack)
      player.swing(InteractionHand.MAIN_HAND)
    }
    new InteractionResultHolder(InteractionResult.sidedSuccess(level.isClientSide), stack)
  }
}
