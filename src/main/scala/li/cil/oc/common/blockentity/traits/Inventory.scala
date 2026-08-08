package li.cil.oc.common.blockentity.traits

import li.cil.oc.api.Persistable
import li.cil.oc.Settings

import java.util.function.Consumer
import li.cil.oc.common.container
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.InventoryUtils
import net.minecraft.core.component.DataComponentHolder
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.{CompoundTag, ListTag, Tag}
import net.minecraft.core.{Direction, HolderLookup}
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.common.MutableDataComponentHolder

trait Inventory extends BaseBlockEntity with container.Inventory {
  private final val InventoryTag = Settings.namespace + "items"
  private lazy val inventory = Array.fill[ItemStack](getContainerSize)(ItemStack.EMPTY)

  def items = inventory

  // ----------------------------------------------------------------------- //

  override def getDisplayName: Component = super[Inventory].getDisplayName

  override def loadComponentsForServer(holder: DataComponentHolder): Unit = {
    super.loadComponentsForServer(holder)
    loadData(Persistable.holder(this))
  }

  override def saveComponentsForServer(holder: MutableDataComponentHolder): Unit = {
    super.saveComponentsForServer(holder)
    val inner = Persistable.holder(this)
    try {
      saveData(inner)
    } finally {
      inner.close()
    }
  }

  override def loadForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadForServer(nbt, provider)
    if (nbt.contains(InventoryTag, Tag.TAG_LIST)) {
      val saved = nbt.getList(InventoryTag, Tag.TAG_COMPOUND)
      for (slot <- items.indices) updateItems(slot, ItemStack.EMPTY)
      for (index <- 0 until saved.size) {
        val entry = saved.getCompound(index)
        val slot = entry.getInt("slot")
        if (slot >= 0 && slot < items.length) {
          updateItems(slot, ItemStack.parseOptional(provider, entry.getCompound("stack")))
        }
      }
    }
    else loadData(nbt, provider) // compatibility with older component-backed saves.
  }

  override def saveForServer(nbt: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.saveForServer(nbt, provider)
    val saved = new ListTag()
    for (slot <- items.indices if !items(slot).isEmpty) {
      val entry = new CompoundTag()
      entry.putInt("slot", slot)
      entry.put("stack", items(slot).save(provider))
      saved.add(entry)
    }
    nbt.put(InventoryTag, saved)
  }

  // ----------------------------------------------------------------------- //

  override def stillValid(player: Player) =
    if (isMoving) player.distanceToSqr(movingPosition.x, movingPosition.y, movingPosition.z) <= 64
    else player.distanceToSqr(x + 0.5, y + 0.5, z + 0.5) <= 64

  // ----------------------------------------------------------------------- //

  def forAllLoot(dst: Consumer[ItemStack]): Unit = InventoryUtils.forAllSlots(this, dst)

  def dropSlot(slot: Int, count: Int = getMaxStackSize, direction: Option[Direction] = None) =
    InventoryUtils.dropSlot(BlockPosition(x, y, z, getLevel), this, slot, count, direction)

  def dropAllSlots() =
    InventoryUtils.dropAllSlots(BlockPosition(x, y, z, getLevel), this)

  def spawnStackInWorld(stack: ItemStack, direction: Option[Direction] = None) =
    InventoryUtils.spawnStackInWorld(BlockPosition(x, y, z, getLevel), stack, direction)
}
