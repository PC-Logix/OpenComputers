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

  addSlotToContainer(80, 35, Slot.Upgrade)
  addPlayerInventorySlots(8, 84)
}
