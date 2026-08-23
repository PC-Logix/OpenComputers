package li.cil.oc.common.container

import li.cil.oc.api.Driver
import li.cil.oc.common.{blockentity, Slot}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player

class DiskDriveMountableInventory private(private val player: Player, private val hand: InteractionHand, private val stack: ItemStack) extends ItemStackInventory {
  def this(player: Player, hand: InteractionHand) = this(player, hand, player.getItemInHand(hand))

  override def getContainerSize = 1

  override protected def inventoryName = "diskdrive"

  override def getMaxStackSize = 1

  override def canPlaceItem(slot: Int, stack: ItemStack): Boolean = (slot, Option(Driver.driverFor(stack, classOf[blockentity.DiskDrive]))) match {
    case (0, Some(driver)) => driver.slot(stack) == Slot.Floppy
    case _ => false
  }

  // The container is just the stack the player is currently holding. This is read in ItemStackInventory's constructor,
  // so we have to make sure this is initialised before then, hence the funny constructor dance.
  override def container: ItemStack = stack

  override def stillValid(player: Player): Boolean = player == this.player && player.getItemInHand(hand) == stack
}
