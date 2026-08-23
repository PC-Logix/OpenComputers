package li.cil.oc.common.menu

import li.cil.oc.{api, Settings}
import li.cil.oc.common.{blockentity, Tier}
import li.cil.oc.common.template.DisassemblerTemplates
import li.cil.oc.util.ItemUtils
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

class Disassembler(id: Int, playerInventory: Inventory, val disassembler: Container)
  extends AbstractMenu(MenuTypes.DISASSEMBLER.get(), id, playerInventory, disassembler) {

  private def allowDisassembling(stack: ItemStack) = {
    val tag = ItemUtils.getTag(stack)
    !stack.isEmpty && (tag == null || !tag.getBoolean(Settings.namespace + "undisassemblable"))
  }

  override protected def getHostClass = classOf[blockentity.Disassembler]

  addSlot(new StaticComponentSlot(this, otherInventory, slots.size, 80, 35, getHostClass, "ocitem", Tier.Any) {
    override def mayPlace(stack: ItemStack): Boolean = {
      if (!container.canPlaceItem(getSlotIndex, stack)) return false
      allowDisassembling(stack) &&
        (((Settings.get.disassembleAllTheThings || api.Items.get(stack) != null) &&
            ItemUtils.getIngredients(playerInventory.player.level.getRecipeManager, stack).nonEmpty) ||
          DisassemblerTemplates.select(stack).isDefined)
    }
  })
  addPlayerInventorySlots(8, 84)

  def disassemblyProgress = synchronizedData.getDouble("disassemblyProgress")

  override protected def detectCustomDataChanges(nbt: CompoundTag): Unit = {
    disassembler match {
      case te: blockentity.Disassembler => synchronizedData.putDouble("disassemblyProgress", te.progress)
      case _ =>
    }
    super.detectCustomDataChanges(nbt)
  }
}
