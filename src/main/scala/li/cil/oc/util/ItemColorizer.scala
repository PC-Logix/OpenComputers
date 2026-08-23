package li.cil.oc.util

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

/**
  * @author asie, Vexatos
  */
object ItemColorizer {
  /**
    * Return whether the specified armor ItemStack has a color.
    */
  def hasColor(stack: ItemStack): Boolean = {
    val tag = ItemUtils.getTag(stack)
    tag != null && tag.contains("display") && tag.getCompound("display").contains("color")
  }

  /**
    * Return the color for the specified armor ItemStack.
    */
  def getColor(stack: ItemStack): Int = {
    val tag = ItemUtils.getTag(stack)
    if (tag != null) {
      if (tag.contains("display")) {
        val displayTag = tag.getCompound("display")
        if (displayTag.contains("color")) displayTag.getInt("color") else -1
      }
      else -1
    }
    else -1
  }

  def removeColor(stack: ItemStack): Unit = {
    val tag = ItemUtils.getTag(stack)
    if (tag != null) {
      val displayTag = tag.getCompound("display")
      if (displayTag.contains("color")) displayTag.remove("color")
      if (displayTag.isEmpty) tag.remove("display")
      CustomData.set(DataComponents.CUSTOM_DATA, stack, if(tag.isEmpty) new CompoundTag() else tag)
    }
  }

  def setColor(stack: ItemStack, color: Int): Unit = {
    CustomData.update(DataComponents.CUSTOM_DATA, stack, data => {
      if (!data.contains("display")) {
        data.put("display", new CompoundTag())
      }

      val display = data.getCompound("display")
      display.putInt("color", color)
    })
  }
}
