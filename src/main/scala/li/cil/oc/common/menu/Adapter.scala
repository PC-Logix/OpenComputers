package li.cil.oc.common.menu

import li.cil.oc.api.Driver
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.blockentity
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.inventory.AbstractContainerMenu

class Adapter(id: Int, playerInventory: Inventory, adapter: Container)
  extends AbstractMenu(MenuTypes.ADAPTER.get(), id, playerInventory, adapter) {

  override protected def getHostClass = classOf[blockentity.Adapter]

  // Keep the upgrade-slot appearance, but also accept card drivers. The
  // inventory performs the authoritative host-aware check for both kinds.
  addSlot(new StaticComponentSlot(this, otherInventory, slots.size, 80, 35, getHostClass, Slot.Upgrade, Tier.Any) {
    override def mayPlace(stack: net.minecraft.world.item.ItemStack): Boolean = {
      if (!otherInventory.canPlaceItem(getSlotIndex, stack)) false
      else Option(Driver.driverFor(stack, getHostClass)).exists { driver =>
        val driverSlot = driver.slot(stack)
        driverSlot == Slot.Upgrade || driverSlot == Slot.Card
      }
    }
  })
  addPlayerInventorySlots(8, 84)
}
