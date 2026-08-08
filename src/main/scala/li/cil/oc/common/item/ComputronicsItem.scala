package li.cil.oc.common.item

import li.cil.oc.Constants
import li.cil.oc.api
import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level

/** Content-only Computronics item port. */
class ComputronicsItem(props: Item.Properties) extends Item(props) {
  private val PortableState = "computronicsPortableDrivePlaying"

  override def use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder[ItemStack] = {
    val stack = player.getItemInHand(hand)
    val info = api.Items.get(stack)
    if (info != null && info.name == Constants.ItemName.ComputronicsPortableTapeDrive && !level.isClientSide) {
      val data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
      data.putBoolean(PortableState, !data.getBoolean(PortableState))
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data))
      player.displayClientMessage(net.minecraft.network.chat.Component.literal(
        if (data.getBoolean(PortableState)) "Portable tape drive: playing" else "Portable tape drive: stopped"), true)
      InteractionResultHolder.sidedSuccess(stack, false)
    } else InteractionResultHolder.pass(stack)
  }
}
