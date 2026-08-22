package li.cil.oc.common.template

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.internal
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item.data.DroneData
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.util.ItemUtils
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

import scala.collection.JavaConverters.asJavaIterable
import scala.collection.convert.ImplicitConversionsToJava._
import scala.jdk.CollectionConverters.IterableHasAsJava

object DroneTemplate extends Template {
  override protected val suggestedComponents = Array(
    "BIOS" -> hasComponent(Constants.ItemName.EEPROM) _)

  override protected def hostClass = classOf[internal.Drone]

  def selectTier1(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.ItemName.DroneCaseTier1)

  def selectTier2(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.ItemName.DroneCaseTier2)

  def selectTier3(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.ItemName.DroneCaseTier3)

  def selectTierCreative(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.ItemName.DroneCaseCreative)

  def validate(inventory: Container): Array[AnyRef] = validateComputer(inventory)

  def assemble(inventory: Container) = {
    val items = (0 until inventory.getContainerSize).map(inventory.getItem)
    val data = new DroneData()
    data.tier = caseTier(inventory)
    data.name = RobotData.randomName
    data.components = items.drop(1).filter(!_.isEmpty).toArray
    data.storedEnergy = Settings.get.bufferDrone.toInt
    val stack = api.Items.get(Constants.ItemName.Drone).createItemStack(1)
    data.saveData(stack)
    val energy = Settings.get.droneBaseCost + complexity(inventory) * Settings.get.droneComplexityCost

    Array(stack, Double.box(energy))
  }

  def selectDisassembler(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.ItemName.Drone)

  def disassemble(stack: ItemStack, ingredients: Array[ItemStack]) = {
    val info = new MicrocontrollerData(stack)
    val itemName = Constants.ItemName.DroneCase(info.tier)

    Array(api.Items.get(itemName).createItemStack(1)) ++ info.components.filter(!_.isEmpty)
  }

  def register(): Unit = {
    // Tier 1
    api.IMC.registerAssemblerTemplate(
      "Drone (Tier 1)",
      "li.cil.oc.common.template.DroneTemplate.selectTier1",
      "li.cil.oc.common.template.DroneTemplate.validate",
      "li.cil.oc.common.template.DroneTemplate.assemble",
      hostClass,
      null,
      Array(
        Tier.Two,
        Tier.One
      ),
      Iterable(
        (Slot.Card, Tier.Two),
        (Slot.Card, Tier.One),
        null,
        (Slot.CPU, Tier.One),
        (Slot.Memory, Tier.One),
        null,
        (Slot.EEPROM, Tier.Any)
      ).map(toPair).asJava)

    // Tier 2
    api.IMC.registerAssemblerTemplate(
      "Drone (Tier 2)",
      "li.cil.oc.common.template.DroneTemplate.selectTier2",
      "li.cil.oc.common.template.DroneTemplate.validate",
      "li.cil.oc.common.template.DroneTemplate.assemble",
      hostClass,
      null,
      Array(
        Tier.Three,
        Tier.Two,
        Tier.One
      ),
      Iterable(
        (Slot.Card, Tier.Two),
        (Slot.Card, Tier.Two),
        null,
        (Slot.CPU, Tier.One),
        (Slot.Memory, Tier.One),
        (Slot.Memory, Tier.One),
        (Slot.EEPROM, Tier.Any)
      ).map(toPair).asJava)

    // Tier 2
    api.IMC.registerAssemblerTemplate(
      "Drone (Tier 3)",
      "li.cil.oc.common.template.DroneTemplate.selectTier3",
      "li.cil.oc.common.template.DroneTemplate.validate",
      "li.cil.oc.common.template.DroneTemplate.assemble",
      hostClass,
      null,
      Array(
        Tier.Four,
        Tier.Three,
        Tier.Two,
        Tier.One
      ),
      Iterable(
        (Slot.Card, Tier.Three),
        (Slot.Card, Tier.Two),
        null,
        (Slot.CPU, Tier.One),
        (Slot.Memory, Tier.Two),
        (Slot.Memory, Tier.One),
        (Slot.EEPROM, Tier.Any)
      ).map(toPair).asJava)

    // Creative
    api.IMC.registerAssemblerTemplate(
      "Drone (Creative)",
      "li.cil.oc.common.template.DroneTemplate.selectTierCreative",
      "li.cil.oc.common.template.DroneTemplate.validate",
      "li.cil.oc.common.template.DroneTemplate.assemble",
      hostClass,
      null,
      Array(
        Tier.Four,
        Tier.Four,
        Tier.Four,
        Tier.Four,
        Tier.Four,
        Tier.Four,
        Tier.Four,
        Tier.Four,
        Tier.Four
      ),
      Iterable(
        (Slot.Card, Tier.Four),
        (Slot.Card, Tier.Four),
        (Slot.Card, Tier.Four),
        (Slot.CPU, Tier.Four),
        (Slot.Memory, Tier.Four),
        (Slot.Memory, Tier.Four),
        (Slot.EEPROM, Tier.Any)
      ).map(toPair).asJava)

    // Disassembler
    api.IMC.registerDisassemblerTemplate(
      "Drone",
      "li.cil.oc.common.template.DroneTemplate.selectDisassembler",
      "li.cil.oc.common.template.DroneTemplate.disassemble")
  }

  override protected def maxComplexity(inventory: Container) =
    if (caseTier(inventory) == Tier.Two) 8
    else if (caseTier(inventory) == Tier.Five) 9001 // Creative
    else 5

  override protected def caseTier(inventory: Container) = ItemUtils.caseTier(inventory.getItem(0))
}
