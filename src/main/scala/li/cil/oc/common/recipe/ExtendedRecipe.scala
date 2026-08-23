package li.cil.oc.common.recipe

import java.util.UUID
import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.ImmutableItemStack
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.common.Loot
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.common.item.data.DroneData
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.server.machine.luac.LuaStateFactory
import li.cil.oc.util.ExtendedDataComponentHolder._
import li.cil.oc.util.{ItemUtils, SideTracker}
import net.minecraft.core.component.DataComponents
import net.minecraft.core.HolderLookup
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Items
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.{CompoundTag, StringTag}
import net.minecraft.world.item.component.CustomData
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.{BlockTags, ItemTags}
import net.minecraft.world.item.crafting.{CraftingInput, Recipe}

import scala.collection.convert.ImplicitConversionsToScala._
import scala.util.control.Breaks._

object ExtendedRecipe {
  private lazy val drone = api.Items.get(Constants.ItemName.Drone)
  private lazy val eeprom = api.Items.get(Constants.ItemName.EEPROM)
  private lazy val mcu = api.Items.get(Constants.BlockName.Microcontroller)
  private lazy val navigationUpgrade = api.Items.get(Constants.ItemName.NavigationUpgrade)
  private lazy val linkedCard = api.Items.get(Constants.ItemName.LinkedCard)
  private lazy val floppy = api.Items.get(Constants.ItemName.Floppy)
  private lazy val drives = Array(
    api.Items.get(Constants.ItemName.HDDTier1),
    api.Items.get(Constants.ItemName.HDDTier2),
    api.Items.get(Constants.ItemName.HDDTier3),
    api.Items.get(Constants.ItemName.HDDTier4),
    api.Items.get(Constants.ItemName.SSDTier1),
    api.Items.get(Constants.ItemName.SSDTier2),
    api.Items.get(Constants.ItemName.SSDTier3)
  )
  private lazy val cpus = Array(
    api.Items.get(Constants.ItemName.CPUTier1),
    api.Items.get(Constants.ItemName.CPUTier2),
    api.Items.get(Constants.ItemName.CPUTier3),
    api.Items.get(Constants.ItemName.APUTier1),
    api.Items.get(Constants.ItemName.APUTier2)
  )
  private lazy val robot = api.Items.get(Constants.BlockName.Robot)
  private lazy val tablet = api.Items.get(Constants.ItemName.Tablet)
  private lazy val print = api.Items.get(Constants.BlockName.Print)
  val beaconBlocks = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "beacon_base_blocks"))

  def initializeStaticResultData(recipe: Recipe[_], resultStack: ItemStack): Unit = {
    val resultItemName = api.Items.get(resultStack)
    val tag = ItemUtils.getTag(resultStack)

    // EEPROM recipe JSON stores script paths in custom data. Resolve them
    // only when the recipe is assembled, not while datapacks are being decoded.
    if (resultItemName == eeprom &&
      resultStack.getCount == 1 && tag != null &&
      recipe.getIngredients.size == 2) {
      val nbt = tag.getCompound(Settings.namespace + "data")

      val labelNbt = nbt.get(Settings.namespace + "label")
      if (labelNbt != null && labelNbt.getType == StringTag.TYPE) {
        resultStack.set(OCComponents.LABEL, labelNbt.asInstanceOf[StringTag].getAsString)
      }
      if (nbt.contains(Settings.namespace + "readonly")) {
        resultStack.set(OCComponents.READONLY, nbt.getBoolean(Settings.namespace + "readonly"))
      }

      val codeNbt = nbt.get(Settings.namespace + "eeprom")
      if (codeNbt != null && codeNbt.getType == StringTag.TYPE) {
        val codePath = codeNbt.asInstanceOf[StringTag].getAsString
        val defaultEEPROM = if (codePath == "bios.lua") Loot.defaultEEPROM else ItemStack.EMPTY
        if (!defaultEEPROM.isEmpty && defaultEEPROM.has(OCComponents.EEPROM_CODE.get())) {
          resultStack.set(OCComponents.EEPROM_CODE, defaultEEPROM.get(OCComponents.EEPROM_CODE.get()))
        }
      }

      val dataNbt = nbt.get(Settings.namespace + "userdata")
      if (dataNbt != null && dataNbt.getType == StringTag.TYPE) {
        val dataPath = dataNbt.asInstanceOf[StringTag].getAsString
        val defaultEEPROM = if (dataPath == "bios.lua") Loot.defaultEEPROM else ItemStack.EMPTY
        if (!defaultEEPROM.isEmpty && defaultEEPROM.has(OCComponents.EEPROM_DATA.get())) {
          resultStack.set(OCComponents.EEPROM_DATA, defaultEEPROM.get(OCComponents.EEPROM_DATA.get()))
        }
      }

      // The custom data above is recipe-only initialization metadata. Keeping
      // it would make JEI treat this result as a different EEPROM subtype from
      // the registered Lua BIOS stack, whose state is stored in components.
      resultStack.remove(DataComponents.CUSTOM_DATA)
    }
  }

  def addNBTToResult(recipe: Recipe[_], craftedStack: ItemStack, inventory: CraftingInput, provider: HolderLookup.Provider): ItemStack = {
    initializeStaticResultData(recipe, craftedStack)
    val craftedItemName = api.Items.get(craftedStack)

    if (craftedItemName == navigationUpgrade) {
      for (stack <- getItems(inventory)) {
        if (stack.getItem == Items.FILLED_MAP) {
          // Store information of the map used for crafting in the result.
          craftedStack.setComponent(OCComponents.SOURCE_MAP_ITEM, ImmutableItemStack.copyOf(stack))
        }
      }
    }

    if (craftedItemName == linkedCard) {
      if (SideTracker.isServer) {
        craftedStack.setComponent(OCComponents.TUNNEL, UUID.randomUUID().toString)
      }
    }

    if (cpus.contains(craftedItemName)) {
      LuaStateFactory.setDefaultArch(craftedStack)
    }

    if (craftedItemName == floppy || drives.contains(craftedItemName)) {
      CustomData.update(DataComponents.CUSTOM_DATA, craftedStack, nbt => {
        if (recipe.canCraftInDimensions(1, 1)) {
          // Formatting / loot to normal disk conversion, only keep coloring.
          val colorKey = Settings.namespace + "color"
          for (stack <- getItems(inventory)) {
            val oldData = ItemUtils.getTag(stack)
            if (api.Items.get(stack) != null && (api.Items.get(stack) == floppy || api.Items.get(stack).name == "lootDisk") && oldData != null) {
              if (oldData.contains(colorKey) && oldData.getInt(colorKey) != DyeColor.LIGHT_GRAY.getId) {
                nbt.put(colorKey, oldData.get(colorKey).copy())
              }
            }
          }
        }
        else if (getItems(inventory).forall(api.Items.get(_) == floppy)) {
          // Copy operation.
          for (stack <- getItems(inventory)) {
            val oldData = ItemUtils.getTag(stack)
            if (api.Items.get(stack) == floppy && oldData != null) {
              for (oldTagName <- oldData.getAllKeys.map(_.asInstanceOf[String]) if !nbt.contains(oldTagName)) {
                nbt.put(oldTagName, oldData.get(oldTagName).copy())
              }
            }
          }
        }
      })
    }

    if (craftedItemName == print &&
      recipe.getIngredients.size == 2) {
      // First, copy old data.
      val data = new PrintData(craftedStack)
      val inputs = getItems(inventory)
      for (stack <- inputs) {
        if (api.Items.get(stack) == print) {
          data.loadData(stack)
        }
      }

      // Then apply new data.
      val glowstoneDust = new ItemStack(Items.GLOWSTONE_DUST)
      val glowstone = new ItemStack(Blocks.GLOWSTONE)
      for (stack <- inputs) {
        if (stack.is(beaconBlocks)) {
          if (data.isBeaconBase) {
            // Crafting wouldn't change anything, prevent accidental resource loss.
            return ItemStack.EMPTY
          }
          data.isBeaconBase = true
        }
        if (ItemStack.isSameItem(glowstoneDust, stack)) {
          if (data.lightLevel == 15) {
            // Crafting wouldn't change anything, prevent accidental resource loss.
            return ItemStack.EMPTY
          }
          data.lightLevel = math.min(15, data.lightLevel + 1)
        }
        if (ItemStack.isSameItem(glowstone, stack)) {
          if (data.lightLevel == 15) {
            // Crafting wouldn't change anything, prevent accidental resource loss.
            return ItemStack.EMPTY
          }
          data.lightLevel = math.min(15, data.lightLevel + 4)
        }
      }

      // Finally apply modified data.
      data.saveData(craftedStack)
    }

    // EEPROM copying.
    if (craftedItemName == eeprom &&
      craftedStack.getCount == 2 &&
      recipe.getIngredients.size == 2) breakable {
      for (stack <- getItems(inventory)) {
        val copy = ItemUtils.getTag(stack)
        if (api.Items.get(stack) == eeprom && copy != null) {
          // Erase node address, just in case.
          copy.getCompound(Settings.namespace + "data").getCompound("node").remove("address")
          CustomData.set(DataComponents.CUSTOM_DATA, craftedStack, copy)
          break()
        }
      }
    }

    // Swapping EEPROM in devices.
    recraft(provider, craftedStack, inventory, mcu, stack => new MCUDataWrapper(stack))
    recraft(provider, craftedStack, inventory, drone, stack => new DroneDataWrapper(stack))
    recraft(provider, craftedStack, inventory, robot, stack => new RobotDataWrapper(stack))
    recraft(provider, craftedStack, inventory, tablet, stack => new TabletDataWrapper(stack))

    craftedStack
  }

  private def getItems(inventory: CraftingInput) = (0 until inventory.size).map(inventory.getItem).filter(!_.isEmpty)

  private def recraft(provider: HolderLookup.Provider, craftedStack: ItemStack, inventory: CraftingInput, descriptor: ItemInfo, dataFactory: (ItemStack) => ItemDataWrapper): Unit = {
    if (api.Items.get(craftedStack) == descriptor) {
      // Find old Microcontroller.
      getItems(inventory).find(api.Items.get(_) == descriptor) match {
        case Some(oldMcu) =>
          val data = dataFactory(oldMcu)

          // Remove old EEPROM.
          val oldRom = data.components.filter(api.Items.get(_) == eeprom)
          data.components = data.components.diff(oldRom)

          // Insert new EEPROM.
          for (stack <- getItems(inventory)) {
            if (api.Items.get(stack) == eeprom) {
              data.components :+= stack.copy.split(1)
            }
          }

          data.save(craftedStack, provider)
        case _ =>
      }
    }
  }

  private trait ItemDataWrapper {
    def components: Array[ItemStack]

    def components_=(value: Array[ItemStack]): Unit

    def save(stack: ItemStack, provider: HolderLookup.Provider): Unit
  }

  private class MCUDataWrapper(val stack: ItemStack) extends ItemDataWrapper {
    private val data = new MicrocontrollerData(stack)

    override def components: Array[ItemStack] = data.components

    override def components_=(value: Array[ItemStack]): Unit = data.components = value

    override def save(stack: ItemStack, provider: HolderLookup.Provider): Unit = data.saveData(stack)
  }

  private class DroneDataWrapper(val stack: ItemStack) extends ItemDataWrapper {
    private val data = new DroneData(stack)

    override def components: Array[ItemStack] = data.components

    override def components_=(value: Array[ItemStack]): Unit = data.components = value

    override def save(stack: ItemStack, provider: HolderLookup.Provider): Unit = data.saveData(stack)
  }

  private class RobotDataWrapper(val stack: ItemStack) extends ItemDataWrapper {
    private val data = new RobotData(stack)

    override def components: Array[ItemStack] = data.components

    override def components_=(value: Array[ItemStack]): Unit = data.components = value

    override def save(stack: ItemStack, provider: HolderLookup.Provider): Unit = data.saveData(stack)
  }

  private class TabletDataWrapper(val stack: ItemStack) extends ItemDataWrapper {
    private val data = new TabletData(stack)

    private var _components: Array[ItemStack] = data.items.filter(!_.isEmpty)

    override def components: Array[ItemStack] = _components

    override def components_=(value: Array[ItemStack]): Unit = {
      _components = value
    }

    override def save(stack: ItemStack, provider: HolderLookup.Provider): Unit = {
      data.items = _components.clone()
      data.saveData(stack)
    }
  }

}
