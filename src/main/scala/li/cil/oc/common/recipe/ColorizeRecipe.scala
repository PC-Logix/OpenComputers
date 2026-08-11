package li.cil.oc.common.recipe

import li.cil.oc.util.Color
import li.cil.oc.util.ItemColorizer
import li.cil.oc.util.StackOption
import net.minecraft.core.HolderLookup
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor.ARGB32
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.crafting.{CraftingBookCategory, CraftingInput, CustomRecipe}
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.{ItemLike, Level}

/**
  * @author asie, Vexatos
  */
class ColorizeRecipe(target: ItemLike) extends CustomRecipe(CraftingBookCategory.MISC) {
  val targetItem: Item = target.asItem()

  override def matches(crafting: CraftingInput, level: Level): Boolean = {
    val stacks = (0 until crafting.size).flatMap(i => StackOption(crafting.getItem(i)))
    val targets = stacks.filter(stack => stack.getItem == targetItem)
    val other = stacks.filterNot(targets.contains(_))
    targets.size == 1 && other.nonEmpty && other.forall(Color.isDye)
  }

  override def assemble(crafting: CraftingInput, provider: HolderLookup.Provider): ItemStack = {
    var targetStack: ItemStack = ItemStack.EMPTY
    val color = Array[Int](0, 0, 0)
    var colorCount = 0
    var maximum = 0

    (0 until crafting.size).flatMap(i => StackOption(crafting.getItem(i))).foreach { stack =>
      if (stack.getItem == targetItem) {
        targetStack = stack.copy()
        targetStack.setCount(1)
      } else {
        val dye = Color.findDye(stack)
        if (dye.isEmpty)
          return ItemStack.EMPTY

        val itemColor = Color.byTag(dye.get).getTextureDiffuseColor
        val red = ARGB32.red(itemColor)
        val green = ARGB32.green(itemColor)
        val blue = ARGB32.blue(itemColor)
        maximum += Math.max(red, Math.max(green, blue))
        color(0) += red
        color(1) += green
        color(2) += blue
        colorCount = colorCount + 1
      }
    }

    if (targetStack.isEmpty) return ItemStack.EMPTY

    if (targetItem == targetStack.getItem) {
      if (ItemColorizer.hasColor(targetStack)) {
        val itemColor = ItemColorizer.getColor(targetStack)
        val red = (itemColor >> 16 & 255).toFloat / 255.0F
        val green = (itemColor >> 8 & 255).toFloat / 255.0F
        val blue = (itemColor & 255).toFloat / 255.0F
        maximum = (maximum.toFloat + Math.max(red, Math.max(green, blue)) * 255.0F).toInt
        color(0) = (color(0).toFloat + red * 255.0F).toInt
        color(1) = (color(1).toFloat + green * 255.0F).toInt
        color(2) = (color(2).toFloat + blue * 255.0F).toInt
        colorCount = colorCount + 1
      }
    }

    var red = color(0) / colorCount
    var green = color(1) / colorCount
    var blue = color(2) / colorCount
    val max = maximum.toFloat / colorCount.toFloat
    val div = Math.max(red, Math.max(green, blue)).toFloat
    red = (red.toFloat * max / div).toInt
    green = (green.toFloat * max / div).toInt
    blue = (blue.toFloat * max / div).toInt
    ItemColorizer.setColor(targetStack, (red << 16) | (green << 8) | blue)
    targetStack
  }

  override def canCraftInDimensions(width: Int, height: Int): Boolean = width * height >= 2

  override def getSerializer = Recipes.COLORIZE.get
}
