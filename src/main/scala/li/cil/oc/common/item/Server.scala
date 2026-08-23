package li.cil.oc.common.item

import li.cil.oc.common.container.ServerInventory
import li.cil.oc.common.menu.MenuTypes
import li.cil.oc.util.Tooltip
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.{Item, ItemStack, TooltipFlag}
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.level.Level
import net.minecraft.world.{InteractionHand, InteractionResultHolder}
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.common.extensions.IItemExtension

import java.util
import scala.collection.mutable

class Server(props: Properties, val tier: Int) extends Item(props) with traits.SimpleItem with IItemExtension {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  private object HelperInventory extends ServerInventory {
    var container = ItemStack.EMPTY

    override def rackSlot = -1
  }

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[Component], flag: TooltipFlag): Unit = {
    super.tooltipExtended(stack, tooltip, flag)
    if (Tooltip.showExtendedTooltip(flag)) {
      HelperInventory.container = stack
      HelperInventory.reinitialize()
      val stacks = mutable.Map.empty[String, Int]
      for (aStack <- (0 until HelperInventory.getContainerSize).map(HelperInventory.getItem) if !aStack.isEmpty) {
        val displayName = aStack.getHoverName.getString
        stacks += displayName -> (if (stacks.contains(displayName)) stacks(displayName) + 1 else 1)
      }
      if (stacks.nonEmpty) {
        Tooltip.add(tooltip, flag, "server.Components")
        for (itemName <- stacks.keys.toArray.sorted) {
          tooltip.add(Component.literal("- " + stacks(itemName) + "x " + itemName).setStyle(Tooltip.DefaultStyle))
        }
      }
    }
  }

  override def use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder[ItemStack] = {
    val stack = player.getItemInHand(hand)
    if (!player.isCrouching) {
      if (!level.isClientSide) player match {
        case srvPlr: ServerPlayer => MenuTypes.openServerGui(srvPlr, new ServerInventory {
            override def container = stack

            override def rackSlot = -1

            override def stillValid(player: Player) = player == srvPlr
          }, -1)
        case _ =>
      }
    }
    InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
  }

}
