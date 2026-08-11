package li.cil.oc.common.recipe

import li.cil.oc.{api, Constants}
import li.cil.oc.common.Loot
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.StackOption
import net.minecraft.core.{HolderLookup, NonNullList}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting._
import net.minecraft.world.level.Level

import scala.collection.immutable

class LootDiskCyclingRecipe(val bookCategory: CraftingBookCategory) extends CustomRecipe(bookCategory) {
  override def matches(crafting: CraftingInput, level: Level): Boolean = {
    val stacks = collectStacks(crafting).toArray
    stacks.length == 2 && stacks.exists(Loot.isLootDisk) && stacks.exists(Wrench.isWrench)
  }

  override def getSerializer: RecipeSerializer[_] = Recipes.LOOTDISK_CYCLING.get()

  override def assemble(crafting: CraftingInput, provider: HolderLookup.Provider): ItemStack = {
    val lootDiskStacks = Loot.disksForCycling
    collectStacks(crafting).find(Loot.isLootDisk) match {
      case Some(lootDisk) if lootDiskStacks.nonEmpty =>
        val lootFactoryName = getLootFactoryName(lootDisk)
        val oldIndex = lootDiskStacks.indexWhere(s => getLootFactoryName(s) == lootFactoryName)
        val newIndex = (oldIndex + 1) % lootDiskStacks.length
        lootDiskStacks(newIndex).copy()
      case _ => ItemStack.EMPTY
    }
  }

  def getLootFactoryName(stack: ItemStack): ResourceLocation = stack.get(OCComponents.LOOT_DISK.get())

  def collectStacks(crafting: CraftingInput): immutable.IndexedSeq[ItemStack] = (0 until crafting.size()).flatMap(i => StackOption(crafting.getItem(i)))

  override def canCraftInDimensions(width: Int, height: Int): Boolean = width * height >= 2

  override def getResultItem(provider: HolderLookup.Provider) = Loot.disksForCycling.headOption match {
    case Some(lootDisk) => lootDisk
    case _ => ItemStack.EMPTY
  }

  override def getRemainingItems(crafting: CraftingInput): NonNullList[ItemStack] = {
    val result = NonNullList.withSize[ItemStack](crafting.size, ItemStack.EMPTY)
    for (slot <- 0 until crafting.size()) {
      val stack = crafting.getItem(slot)
      if (Wrench.isWrench(stack)) {
        result.set(slot, stack.copy())
        stack.setCount(0)
      }
    }
    result
  }

  override def getIngredients = {
    val ingredients = NonNullList.create[Ingredient]
    ingredients.add(Ingredient.of(Loot.disksForCycling.toArray: _*))
    ingredients.add(Ingredient.of(api.Items.get(Constants.ItemName.Wrench).createItemStack(1)))
    ingredients
  }
}
