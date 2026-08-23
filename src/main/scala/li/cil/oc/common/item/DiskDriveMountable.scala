package li.cil.oc.common.item

import li.cil.oc.common.container.DiskDriveMountableInventory
import li.cil.oc.common.menu.{DiskDrive => DiskDriveContainer}
import net.minecraft.world.{InteractionHand, InteractionResultHolder, SimpleMenuProvider}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.level.Level
import net.neoforged.neoforge.common.extensions.IItemExtension

class DiskDriveMountable(props: Properties) extends Item(props) with traits.SimpleItem with IItemExtension {
  override def use(level: Level, player: Player, hand: InteractionHand) = {
    val stack = player.getItemInHand(hand)
    if (!level.isClientSide) {
      val container = new DiskDriveMountableInventory(player, hand)
      player.openMenu(new SimpleMenuProvider((id, inv, _) => new DiskDriveContainer(id, inv, container), container.getDisplayName))
    }
    InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
  }
}
