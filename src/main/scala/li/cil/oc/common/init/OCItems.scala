package li.cil.oc.common.init

import li.cil.oc.api.detail.{ItemAPI, ItemInfo}
import li.cil.oc.api.fs.FileSystem
import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.common.item.data._
import li.cil.oc.common.item.traits.SimpleItem
import li.cil.oc.common.{Loot, Tier, item}
import li.cil.oc.server.machine.luac.LuaStateFactory
import li.cil.oc.util.{Rarity => OCRarity}
import li.cil.oc.{Constants, OpenComputers, Settings, common}
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item._
import net.minecraft.world.level.block.Block
import net.neoforged.bus.api.{EventPriority, IEventBus}
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.{DeferredItem, DeferredRegister, RegisterEvent}

import java.nio.ByteBuffer
import java.util.concurrent.Callable
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object OCItems extends ItemAPI {
  private val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(Settings.resourceDomain)

  val descriptors = mutable.LinkedHashMap.empty[String, ItemInfo]

  val names = mutable.Map.empty[Any, String]

  val aliases = Map(
    "datacard" -> Constants.ItemName.DataCardTier1,
    "wlancard" -> Constants.ItemName.WirelessNetworkCardTier2
  )

  override def get(name: String): ItemInfo = descriptors.get(aliases.get(name).getOrElse(name)).orNull

  override def get(stack: ItemStack): ItemInfo = names.get(getBlockOrItem(stack)) match {
    case Some(name) => get(name)
    case _ => null
  }

  def registerBlockOnly[T <: Block](instance: T, id: String): T = {
    if (!descriptors.contains(id)) {
      instance match {
        case simple: SimpleBlock =>
          simple.setUnlocalizedName("oc." + id)
        case _ =>
      }
      descriptors += id -> new ItemInfo {
        override def name: String = id

        override def block = instance

        override def item = null

        override def createItemStack(size: Int): ItemStack = {
          OpenComputers.log.warn(s"Attempt to get ItemStack for block ${instance} without item form")
          ItemStack.EMPTY
        }
      }
      names += instance -> id
    }
    instance
  }

  def registerBlock[T <: Block](instance: T, id: String, itemProps: Properties): T = {
    if (!descriptors.contains(id)) {
      instance match {
        case simple: SimpleBlock =>
          simple.setUnlocalizedName("oc." + id)

          val ro: DeferredItem[Item] = ITEMS.register(id, () => {
            val itemInst: Item = new common.block.Item(simple, itemProps)
            OpenComputers.proxy.registerModel(itemInst, id)
            itemInst
          })

          descriptors += id -> new ItemInfo {
            override def name: String = id

            override def block = instance

            override def item = ro.get()

            override def createItemStack(size: Int): ItemStack = instance match {
              case simple: SimpleBlock => simple.createItemStack(size)
              case _ => new ItemStack(instance, size)
            }
          }
          names += instance -> id
        case _ =>
      }
    }
    instance
  }

  def registerItem[T <: Item](makeItem: => T, id: String): DeferredItem[T] = {
    if (descriptors.contains(id)) {
      throw new IllegalArgumentException("Duplicate item " + id)
    }
    // Construct items inside the supplier while the registry is writable.
    val ro: DeferredItem[T] = ITEMS.register(id, () => {
      val instance = makeItem
      instance match {
        case simple: SimpleItem =>
          OpenComputers.proxy.registerModel(simple, id)
        case _ =>
      }
      names += instance -> id
      instance
    })
    descriptors += id -> new ItemInfo {
      override def name: String = id

      override def block: Block = null

      override def item: Item = ro.get()

      override def createItemStack(size: Int): ItemStack = ro.get() match {
        case simple: SimpleItem => simple.createItemStack(size)
        case _ => new ItemStack(ro.get(), size)
      }
    }
    ro
  }

  private def registerBasicItem(id: String, props: Item.Properties = defaultProps): DeferredItem[Item] = registerItem(new item.BasicItem(props, id), id)

  private def registerBasicTieredItem(id: String, props: Item.Properties): DeferredItem[Item] = registerItem(new item.BasicTieredItem(props, id), id)

  def registerStack(stack: ItemStack, id: String): ItemStack = {
    val immutableStack = stack.copy()
    descriptors += id -> new ItemInfo {
      override def name: String = id

      override def block = null

      override def createItemStack(size: Int): ItemStack = {
        val copy = immutableStack.copy()
        copy.setCount(size)
        copy
      }

      override def item: Item = immutableStack.getItem
    }
    stack
  }

  private def getBlockOrItem(stack: ItemStack): Any =
    if (stack.isEmpty) null
    else stack.getItem match {
      case block: BlockItem => block.getBlock
      case item => item
    }

  // ----------------------------------------------------------------------- //

  private val registeredItems: ArrayBuffer[ItemStack] = mutable.ArrayBuffer.empty[ItemStack]

  override def registerFloppy(name: String, loc: ResourceLocation, color: DyeColor, factory: Callable[FileSystem], doRecipeCycling: Boolean): ItemStack = {
    val stack = Loot.registerLootDisk(name, loc, color, factory, doRecipeCycling)

    registeredItems += stack

    stack.copy()
  }

  override def registerEEPROM(name: String, code: Array[Byte], data: Array[Byte], readonly: Boolean): ItemStack = {
    val stack = createEEPROM(name, code, data, readonly)
    registeredItems += stack
    stack.copy()
  }

  private def createEEPROM(name: String, code: Array[Byte], data: Array[Byte], readonly: Boolean): ItemStack = {
    val stack = get(Constants.ItemName.EEPROM).createItemStack(1)
    if (name != null) {
      stack.set(OCComponents.LABEL, name.trim.take(24))
    }
    if (code != null) {
      stack.set(OCComponents.EEPROM_CODE, ByteBuffer.wrap(code.take(Settings.get.eepromSize)))
    }
    if (data != null) {
      stack.set(OCComponents.EEPROM_DATA, ByteBuffer.wrap(data.take(Settings.get.eepromDataSize)))
    }
    stack.set(OCComponents.READONLY, readonly)

    stack
  }

  // ----------------------------------------------------------------------- //

  private def safeGetStack(name: String) = Option(get(name)).map(_.createItemStack(1)).getOrElse(ItemStack.EMPTY)

  def createConfiguredDrone(): ItemStack = {
    val data = new DroneData()

    data.name = "Crecopter"
    data.tier = Tier.Five
    data.storedEnergy = Settings.get.bufferDrone.toInt
    data.components = Array(
      safeGetStack(Constants.ItemName.InventoryUpgrade),
      safeGetStack(Constants.ItemName.InventoryUpgrade),
      safeGetStack(Constants.ItemName.InventoryControllerUpgrade),
      safeGetStack(Constants.ItemName.TankUpgrade),
      safeGetStack(Constants.ItemName.TankControllerUpgrade),
      safeGetStack(Constants.ItemName.LeashUpgrade),
      safeGetStack(Constants.ItemName.AngelUpgrade),

      safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),

      LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
      safeGetStack(Constants.ItemName.RAMTier6),
      safeGetStack(Constants.ItemName.RAMTier6)
    ).filter(!_.isEmpty)

    data.createItemStack()
  }

  def createConfiguredMicrocontroller(): ItemStack = {
    val data = new MicrocontrollerData()

    data.tier = Tier.Five
    data.storedEnergy = Settings.get.bufferMicrocontroller.toInt
    data.components = Array(
      safeGetStack(Constants.ItemName.SignUpgrade),
      safeGetStack(Constants.ItemName.PistonUpgrade),

      safeGetStack(Constants.ItemName.RedstoneCardTier1),
      safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),

      LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
      safeGetStack(Constants.ItemName.RAMTier6),
      safeGetStack(Constants.ItemName.RAMTier6)
    ).filter(!_.isEmpty)

    data.createItemStack()
  }

  def createConfiguredRobot(): ItemStack = {
    val data = new RobotData()

    data.name = Component.literal("Creatix")
    data.tier = Tier.Five
    data.robotEnergy = Settings.get.bufferRobot.toInt
    data.totalEnergy = data.robotEnergy
    data.components = Array(
      safeGetStack(Constants.BlockName.ScreenTier1),
      safeGetStack(Constants.BlockName.Keyboard),
      safeGetStack(Constants.BlockName.Geolyzer),
      safeGetStack(Constants.ItemName.InventoryUpgrade),
      safeGetStack(Constants.ItemName.InventoryUpgrade),
      safeGetStack(Constants.ItemName.InventoryUpgrade),
      safeGetStack(Constants.ItemName.InventoryUpgrade),
      safeGetStack(Constants.ItemName.InventoryControllerUpgrade),
      safeGetStack(Constants.ItemName.TankUpgrade),
      safeGetStack(Constants.ItemName.TankControllerUpgrade),
      safeGetStack(Constants.ItemName.CraftingUpgrade),
      safeGetStack(Constants.ItemName.HoverUpgradeTier2),
      safeGetStack(Constants.ItemName.AngelUpgrade),
      safeGetStack(Constants.ItemName.TradingUpgrade),
      safeGetStack(Constants.ItemName.ExperienceUpgrade),

      safeGetStack(Constants.ItemName.GraphicsCardTier3),
      safeGetStack(Constants.ItemName.RedstoneCardTier2),
      safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),
      safeGetStack(Constants.ItemName.InternetCard),

      LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
      safeGetStack(Constants.ItemName.RAMTier6),
      safeGetStack(Constants.ItemName.RAMTier6),

      safeGetStack(Constants.ItemName.LuaBios),
      safeGetStack(Constants.ItemName.OpenOS),
      safeGetStack(Constants.ItemName.HDDTier3)
    ).filter(!_.isEmpty)
    data.containers = Array(
      safeGetStack(Constants.ItemName.CardContainerTier3),
      safeGetStack(Constants.ItemName.UpgradeContainerTier3),
      safeGetStack(Constants.BlockName.DiskDrive)
    ).filter(!_.isEmpty)

    data.createItemStack()
  }

  def createConfiguredTablet(): ItemStack = {
    val data = new TabletData()

    data.tier = Tier.Five
    data.energy = Settings.get.bufferTablet
    data.maxEnergy = data.energy
    data.items = Array(
      safeGetStack(Constants.BlockName.ScreenTier1),
      safeGetStack(Constants.BlockName.Keyboard),

      safeGetStack(Constants.ItemName.SignUpgrade),
      safeGetStack(Constants.ItemName.PistonUpgrade),
      safeGetStack(Constants.BlockName.Geolyzer),
      safeGetStack(Constants.ItemName.NavigationUpgrade),
      safeGetStack(Constants.ItemName.Analyzer),

      safeGetStack(Constants.ItemName.GraphicsCardTier3),
      safeGetStack(Constants.ItemName.RedstoneCardTier2),
      safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),

      LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
      safeGetStack(Constants.ItemName.RAMTier6),
      safeGetStack(Constants.ItemName.RAMTier6),

      safeGetStack(Constants.ItemName.LuaBios),
      safeGetStack(Constants.ItemName.HDDTier3)
    ).padTo(32, ItemStack.EMPTY)
    data.items(31) = safeGetStack(Constants.ItemName.OpenOS)
    data.container = safeGetStack(Constants.BlockName.DiskDrive)

    data.createItemStack()
  }

  def createChargedHoverBoots(): ItemStack = {
    val data = new HoverBootsData()
    data.charge = Settings.get.bufferHoverBoots

    data.createItemStack()
  }

  // ----------------------------------------------------------------------- //

  private def defaultProps = new Properties()

  def init(bus: IEventBus): Unit = {
    // DeferredRegister listens at HIGHEST priority, so our LOW-priority listener
    // runs after all items are registered — safe to call ro.get() / createItemStack.
    bus.addListener(EventPriority.LOW, (event: RegisterEvent) => {
      if (event.getRegistryKey == Registries.ITEM) {
        initPostStorage()
      }
    })

    ITEMS.register(bus)
  }

  /////////////////////////////////////////////////////////////////
  // Crafting materials.
  /////////////////////////////////////////////////////////////////
  val CuttingWire: DeferredItem[Item] = registerBasicItem(Constants.ItemName.CuttingWire)
  val Acid: DeferredItem[item.Acid] = registerItem(new item.Acid(defaultProps), Constants.ItemName.Acid)
  val RawCircuitBoard: DeferredItem[Item] = registerBasicItem(Constants.ItemName.RawCircuitBoard)
  val CircuitBoard: DeferredItem[Item] = registerBasicItem(Constants.ItemName.CircuitBoard)
  val PrintedCircuitBoard: DeferredItem[Item] = registerBasicItem(Constants.ItemName.PrintedCircuitBoard)
  val Card: DeferredItem[Item] = registerItem(new item.BasicItem(defaultProps, "cardbase"), Constants.ItemName.Card)
  val Transistor: DeferredItem[Item] = registerBasicItem(Constants.ItemName.Transistor)
  val ChipTier1: DeferredItem[item.Microchip] = registerItem(new item.Microchip(defaultProps, Tier.One), Constants.ItemName.ChipTier1)
  val ChipTier2: DeferredItem[item.Microchip] = registerItem(new item.Microchip(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.ChipTier2)
  val ChipTier3: DeferredItem[item.Microchip] = registerItem(new item.Microchip(defaultProps.rarity(Rarity.RARE), Tier.Three), Constants.ItemName.ChipTier3)
  val ChipTier4: DeferredItem[item.Microchip] = registerItem(new item.Microchip(defaultProps.rarity(OCRarity.LEGENDARY), Tier.Four), Constants.ItemName.ChipTier4)
  val Alu: DeferredItem[Item] = registerBasicItem(Constants.ItemName.Alu)
  val ControlUnit: DeferredItem[Item] = registerItem(new item.BasicItem(defaultProps, "controlunit"), Constants.ItemName.ControlUnit)
  val Disk: DeferredItem[Item] = registerBasicItem(Constants.ItemName.Disk)
  val Interweb: DeferredItem[Item] = registerBasicItem(Constants.ItemName.Interweb)
  val ButtonGroup: DeferredItem[Item] = registerBasicItem(Constants.ItemName.ButtonGroup, defaultProps)
  val ArrowKeys: DeferredItem[Item] = registerBasicItem(Constants.ItemName.ArrowKeys)
  val NumPad: DeferredItem[Item] = registerBasicItem(Constants.ItemName.NumPad)

  val TabletCaseTier1: DeferredItem[item.TabletCase] = registerItem(new item.TabletCase(defaultProps, Tier.One), Constants.ItemName.TabletCaseTier1)
  val TabletCaseTier2: DeferredItem[item.TabletCase] = registerItem(new item.TabletCase(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.TabletCaseTier2)
  val TabletCaseTier3: DeferredItem[item.TabletCase] = registerItem(new item.TabletCase(defaultProps.rarity(Rarity.RARE), Tier.Three), Constants.ItemName.TabletCaseTier3)
  val TabletCaseCreative: DeferredItem[item.TabletCase] = registerItem(new item.TabletCase(defaultProps.rarity(Rarity.EPIC), Tier.Five), Constants.ItemName.TabletCaseCreative)
  val MicrocontrollerCaseTier1: DeferredItem[item.MicrocontrollerCase] = registerItem(new item.MicrocontrollerCase(defaultProps, Tier.One), Constants.ItemName.MicrocontrollerCaseTier1)
  val MicrocontrollerCaseTier2: DeferredItem[item.MicrocontrollerCase] = registerItem(new item.MicrocontrollerCase(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.MicrocontrollerCaseTier2)
  val MicrocontrollerCaseTier3: DeferredItem[item.MicrocontrollerCase] = registerItem(new item.MicrocontrollerCase(defaultProps.rarity(Rarity.RARE), Tier.Three), Constants.ItemName.MicrocontrollerCaseTier3)
  val MicrocontrollerCaseCreative: DeferredItem[item.MicrocontrollerCase] = registerItem(new item.MicrocontrollerCase(defaultProps.rarity(Rarity.EPIC), Tier.Five), Constants.ItemName.MicrocontrollerCaseCreative)
  val DroneCaseTier1: DeferredItem[item.DroneCase] = registerItem(new item.DroneCase(defaultProps, Tier.One), Constants.ItemName.DroneCaseTier1)
  val DroneCaseTier2: DeferredItem[item.DroneCase] = registerItem(new item.DroneCase(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.DroneCaseTier2)
  val DroneCaseTier3: DeferredItem[item.DroneCase] = registerItem(new item.DroneCase(defaultProps.rarity(OCRarity.LEGENDARY), Tier.Three), Constants.ItemName.DroneCaseTier3)
  val DroneCaseCreative: DeferredItem[item.DroneCase] = registerItem(new item.DroneCase(defaultProps.rarity(Rarity.EPIC), Tier.Five), Constants.ItemName.DroneCaseCreative)

  val InkCartridgeEmpty: DeferredItem[Item] = registerBasicItem(Constants.ItemName.InkCartridgeEmpty, defaultProps.stacksTo(1))
  val InkCartridge: DeferredItem[item.InkCartridge] = registerItem(new item.InkCartridge(defaultProps.stacksTo(1)), Constants.ItemName.InkCartridge)
  val Chamelium: DeferredItem[item.Chamelium] = registerItem(new item.Chamelium(defaultProps), Constants.ItemName.Chamelium)

  val DiamondChip: DeferredItem[Item] = registerItem(new item.BasicItem(defaultProps, "diamondchip"), Constants.ItemName.DiamondChip)
  val NetheriteSilicon: DeferredItem[Item] = registerBasicItem(Constants.ItemName.NetheriteSilicon, defaultProps)

  // All kinds of tools.
  val Analyzer: DeferredItem[item.Analyzer] = registerItem(new item.Analyzer(defaultProps), Constants.ItemName.Analyzer)
  val Debugger: DeferredItem[item.Debugger] = registerItem(new item.Debugger(defaultProps), Constants.ItemName.Debugger)
  val Terminal: DeferredItem[item.Terminal] = registerItem(new item.Terminal(defaultProps.stacksTo(1)), Constants.ItemName.Terminal)
  val TexturePicker: DeferredItem[item.TexturePicker] = registerItem(new item.TexturePicker(defaultProps), Constants.ItemName.TexturePicker)
  val Manual: DeferredItem[item.Manual] = registerItem(new item.Manual(defaultProps), Constants.ItemName.Manual)
  val Wrench: DeferredItem[item.Wrench] = registerItem(new item.Wrench(defaultProps.stacksTo(1)), Constants.ItemName.Wrench)

  // 1.5.11
  val HoverBoots: DeferredItem[item.HoverBoots] = registerItem(new item.HoverBoots(defaultProps.stacksTo(1).rarity(Rarity.UNCOMMON).setNoRepair), Constants.ItemName.HoverBoots)

  // 1.5.18
  val Nanomachines: DeferredItem[item.Nanomachines] = registerItem(new item.Nanomachines(defaultProps.rarity(Rarity.UNCOMMON)), Constants.ItemName.Nanomachines)

  /////////////////////////////////////////////////////////////////
  // General purpose components.
  /////////////////////////////////////////////////////////////////
  val CPUTier1: DeferredItem[item.CPU] = registerItem(new item.CPU(defaultProps, Tier.One), Constants.ItemName.CPUTier1)
  val CPUTier2: DeferredItem[item.CPU] = registerItem(new item.CPU(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.CPUTier2)
  val CPUTier3: DeferredItem[item.CPU] = registerItem(new item.CPU(defaultProps.rarity(Rarity.RARE), Tier.Three), Constants.ItemName.CPUTier3)
  val CPUTier4: DeferredItem[item.CPU] = registerItem(new item.CPU(defaultProps.rarity(OCRarity.LEGENDARY), Tier.Four), Constants.ItemName.CPUTier4)

  val ComponentBusTier1: DeferredItem[item.ComponentBus] = registerItem(new item.ComponentBus(defaultProps, Tier.One), Constants.ItemName.ComponentBusTier1)
  val ComponentBusTier2: DeferredItem[item.ComponentBus] = registerItem(new item.ComponentBus(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.ComponentBusTier2)
  val ComponentBusTier3: DeferredItem[item.ComponentBus] = registerItem(new item.ComponentBus(defaultProps.rarity(Rarity.RARE), Tier.Three), Constants.ItemName.ComponentBusTier3)
  val ComponentBusTier4: DeferredItem[item.ComponentBus] = registerItem(new item.ComponentBus(defaultProps.rarity(OCRarity.LEGENDARY), Tier.Four), Constants.ItemName.ComponentBusTier4)

  val RAMTier1: DeferredItem[item.Memory] = registerItem(new item.Memory(defaultProps, Tier.One), Constants.ItemName.RAMTier1)
  val RAMTier2: DeferredItem[item.Memory] = registerItem(new item.Memory(defaultProps, Tier.Two), Constants.ItemName.RAMTier2)
  val RAMTier3: DeferredItem[item.Memory] = registerItem(new item.Memory(defaultProps.rarity(Rarity.UNCOMMON), Tier.Three), Constants.ItemName.RAMTier3)
  val RAMTier4: DeferredItem[item.Memory] = registerItem(new item.Memory(defaultProps.rarity(Rarity.UNCOMMON), Tier.Four), Constants.ItemName.RAMTier4)
  val RAMTier5: DeferredItem[item.Memory] = registerItem(new item.Memory(defaultProps.rarity(Rarity.RARE), Tier.Five), Constants.ItemName.RAMTier5)
  val RAMTier6: DeferredItem[item.Memory] = registerItem(new item.Memory(defaultProps.rarity(Rarity.RARE), Tier.Six), Constants.ItemName.RAMTier6)
  val RAMTier7: DeferredItem[item.Memory] = registerItem(new item.Memory(defaultProps.rarity(OCRarity.LEGENDARY), Tier.Seven), Constants.ItemName.RAMTier7)
  val RAMTier8: DeferredItem[item.Memory] = registerItem(new item.Memory(defaultProps.rarity(OCRarity.LEGENDARY), Tier.Eight), Constants.ItemName.RAMTier8)

  val ServerCreative: DeferredItem[item.Server] = registerItem(new item.Server(defaultProps.stacksTo(1).rarity(Rarity.EPIC), Tier.Five), Constants.ItemName.ServerCreative)
  val ServerTier1: DeferredItem[item.Server] = registerItem(new item.Server(defaultProps.stacksTo(1), Tier.One), Constants.ItemName.ServerTier1)
  val ServerTier2: DeferredItem[item.Server] = registerItem(new item.Server(defaultProps.stacksTo(1).rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.ServerTier2)
  val ServerTier3: DeferredItem[item.Server] = registerItem(new item.Server(defaultProps.stacksTo(1).rarity(Rarity.RARE), Tier.Three), Constants.ItemName.ServerTier3)
  val ServerTier4: DeferredItem[item.Server] = registerItem(new item.Server(defaultProps.stacksTo(1).rarity(OCRarity.LEGENDARY), Tier.Four), Constants.ItemName.ServerTier4)

  val APUTier1: DeferredItem[item.APU] = registerItem(new item.APU(defaultProps.rarity(Rarity.UNCOMMON), Tier.One), Constants.ItemName.APUTier1)
  val APUTier2: DeferredItem[item.APU] = registerItem(new item.APU(defaultProps.rarity(Rarity.RARE), Tier.Two), Constants.ItemName.APUTier2)
  val APUTier3: DeferredItem[item.APU] = registerItem(new item.APU(defaultProps.rarity(OCRarity.LEGENDARY), Tier.Three), Constants.ItemName.APUTier3)
  val APUCreative: DeferredItem[item.APU] = registerItem(new item.APU(defaultProps.rarity(Rarity.EPIC), Tier.Five), Constants.ItemName.APUCreative)

  // 1.6
  val TerminalServer: DeferredItem[item.TerminalServer] = registerItem(new item.TerminalServer(defaultProps.stacksTo(1)), Constants.ItemName.TerminalServer)
  val RackKVM: DeferredItem[Item] = registerBasicItem(Constants.ItemName.RackKVM, defaultProps.stacksTo(1))
  val DiskDriveMountable: DeferredItem[item.DiskDriveMountable] = registerItem(new item.DiskDriveMountable(defaultProps.stacksTo(1)), Constants.ItemName.DiskDriveMountable)

  // 1.9
  val RAMCreative: DeferredItem[Item] = registerBasicItem(Constants.ItemName.RAMCreative, defaultProps.rarity(Rarity.EPIC))
  val CapacitorMountable: DeferredItem[Item] = registerBasicItem(Constants.ItemName.CapacitorMountable, defaultProps.stacksTo(1))

  // Card components.
  val DebugCard: DeferredItem[item.DebugCard] = registerItem(new item.DebugCard(defaultProps), Constants.ItemName.DebugCard)
  val GraphicsCardTier1: DeferredItem[item.GraphicsCard] = registerItem(new item.GraphicsCard(defaultProps, Tier.One), Constants.ItemName.GraphicsCardTier1)
  val GraphicsCardTier2: DeferredItem[item.GraphicsCard] = registerItem(new item.GraphicsCard(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.GraphicsCardTier2)
  val GraphicsCardTier3: DeferredItem[item.GraphicsCard] = registerItem(new item.GraphicsCard(defaultProps.rarity(Rarity.RARE), Tier.Three), Constants.ItemName.GraphicsCardTier3)
  val GraphicsCardTier4: DeferredItem[item.GraphicsCard] = registerItem(new item.GraphicsCard(defaultProps.rarity(OCRarity.LEGENDARY), Tier.Four), Constants.ItemName.GraphicsCardTier4)
  val QuadGraphicsCard: DeferredItem[Item] = registerItem(new item.QuadGraphicsCard(defaultProps.rarity(OCRarity.LEGENDARY)), Constants.ItemName.QuadGraphicsCard)
  val RedstoneCardTier1: DeferredItem[item.RedstoneCard] = registerItem(new item.RedstoneCard(defaultProps, Tier.One), Constants.ItemName.RedstoneCardTier1)
  val RedstoneCardTier2: DeferredItem[item.RedstoneCard] = registerItem(new item.RedstoneCard(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.RedstoneCardTier2)
  val NetworkCard: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps, "networkcard"), Constants.ItemName.NetworkCard)
  val WirelessNetworkCardTier2: DeferredItem[item.WirelessNetworkCard] = registerItem(new item.WirelessNetworkCard(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.WirelessNetworkCardTier2)
  val InternetCard: DeferredItem[Item] = registerBasicTieredItem(Constants.ItemName.InternetCard, defaultProps.rarity(Rarity.UNCOMMON))
  val LinkedCard: DeferredItem[item.LinkedCard] = registerItem(new item.LinkedCard(defaultProps.rarity(Rarity.RARE)), Constants.ItemName.LinkedCard)

  // 1.5.13
  val DataCardTier1: DeferredItem[item.DataCard] = registerItem(new item.DataCard(defaultProps, Tier.One), Constants.ItemName.DataCardTier1)

  // 1.5.15
  val DataCardTier2: DeferredItem[item.DataCard] = registerItem(new item.DataCard(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.DataCardTier2)
  val DataCardTier3: DeferredItem[item.DataCard] = registerItem(new item.DataCard(defaultProps.rarity(Rarity.RARE), Tier.Three), Constants.ItemName.DataCardTier3)

  // 1.9
  val AudioCardTier1: DeferredItem[item.AudioCard] = registerItem(new item.AudioCard(defaultProps), Constants.ItemName.AudioCardTier1)

  /////////////////////////////////////////////////////////////////
  // Upgrade components.
  /////////////////////////////////////////////////////////////////
  val AngelUpgrade: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps.rarity(Rarity.UNCOMMON), "upgradeangel"), Constants.ItemName.AngelUpgrade)
  val BatteryUpgradeTier1: DeferredItem[item.UpgradeBattery] = registerItem(new item.UpgradeBattery(defaultProps, Tier.One), Constants.ItemName.BatteryUpgradeTier1)
  val BatteryUpgradeTier2: DeferredItem[item.UpgradeBattery] = registerItem(new item.UpgradeBattery(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.BatteryUpgradeTier2)
  val BatteryUpgradeTier3: DeferredItem[item.UpgradeBattery] = registerItem(new item.UpgradeBattery(defaultProps.rarity(Rarity.RARE), Tier.Three), Constants.ItemName.BatteryUpgradeTier3)
  val ChunkloaderUpgrade: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps.rarity(Rarity.RARE), "upgradechunkloader"), Constants.ItemName.ChunkloaderUpgrade)
  val CardContainerTier1: DeferredItem[item.UpgradeContainerCard] = registerItem(new item.UpgradeContainerCard(defaultProps, Tier.One), Constants.ItemName.CardContainerTier1)
  val CardContainerTier2: DeferredItem[item.UpgradeContainerCard] = registerItem(new item.UpgradeContainerCard(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.CardContainerTier2)
  val CardContainerTier3: DeferredItem[item.UpgradeContainerCard] = registerItem(new item.UpgradeContainerCard(defaultProps.rarity(Rarity.RARE), Tier.Three), Constants.ItemName.CardContainerTier3)
  val UpgradeContainerTier1: DeferredItem[item.UpgradeContainerUpgrade] = registerItem(new item.UpgradeContainerUpgrade(defaultProps, Tier.One), Constants.ItemName.UpgradeContainerTier1)
  val UpgradeContainerTier2: DeferredItem[item.UpgradeContainerUpgrade] = registerItem(new item.UpgradeContainerUpgrade(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.UpgradeContainerTier2)
  val UpgradeContainerTier3: DeferredItem[item.UpgradeContainerUpgrade] = registerItem(new item.UpgradeContainerUpgrade(defaultProps.rarity(Rarity.RARE), Tier.Three), Constants.ItemName.UpgradeContainerTier3)
  val CraftingUpgrade: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps.rarity(Rarity.UNCOMMON), "upgradecrafting"), Constants.ItemName.CraftingUpgrade)
  val DatabaseUpgradeTier1: DeferredItem[item.UpgradeDatabase] = registerItem(new item.UpgradeDatabase(defaultProps, Tier.One), Constants.ItemName.DatabaseUpgradeTier1)
  val DatabaseUpgradeTier2: DeferredItem[item.UpgradeDatabase] = registerItem(new item.UpgradeDatabase(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.DatabaseUpgradeTier2)
  val DatabaseUpgradeTier3: DeferredItem[item.UpgradeDatabase] = registerItem(new item.UpgradeDatabase(defaultProps.rarity(Rarity.RARE), Tier.Three), Constants.ItemName.DatabaseUpgradeTier3)
  val ExperienceUpgrade: DeferredItem[item.UpgradeExperience] = registerItem(new item.UpgradeExperience(defaultProps.rarity(Rarity.RARE)), Constants.ItemName.ExperienceUpgrade)
  val GeneratorUpgrade: DeferredItem[item.UpgradeGenerator] = registerItem(new item.UpgradeGenerator(defaultProps.rarity(Rarity.UNCOMMON)), Constants.ItemName.GeneratorUpgrade)
  val InventoryUpgrade: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps, "upgradeinventory"), Constants.ItemName.InventoryUpgrade)
  val InventoryControllerUpgrade: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps.rarity(Rarity.UNCOMMON), "upgradeinventorycontroller"), Constants.ItemName.InventoryControllerUpgrade)
  val NavigationUpgrade: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps.rarity(Rarity.UNCOMMON), "upgradenavigation"), Constants.ItemName.NavigationUpgrade)
  val PistonUpgrade: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps.rarity(Rarity.UNCOMMON), "upgradepiston"), Constants.ItemName.PistonUpgrade)
  val SignUpgrade: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps.rarity(Rarity.UNCOMMON), "upgradesign"), Constants.ItemName.SignUpgrade)
  val SolarGeneratorUpgrade: DeferredItem[item.UpgradeSolarGenerator] = registerItem(new item.UpgradeSolarGenerator(defaultProps.rarity(Rarity.UNCOMMON)), Constants.ItemName.SolarGeneratorUpgrade)
  val TankUpgrade: DeferredItem[item.UpgradeTank] = registerItem(new item.UpgradeTank(defaultProps), Constants.ItemName.TankUpgrade)
  val TankControllerUpgrade: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps.rarity(Rarity.UNCOMMON), "upgradetankcontroller"), Constants.ItemName.TankControllerUpgrade)
  val TractorBeamUpgrade: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps.rarity(Rarity.RARE), "upgradetractorbeam"), Constants.ItemName.TractorBeamUpgrade)
  val LeashUpgrade: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps, "upgradeleash"), Constants.ItemName.LeashUpgrade)

  // 1.5.8
  val HoverUpgradeTier1: DeferredItem[item.UpgradeHover] = registerItem(new item.UpgradeHover(defaultProps, Tier.One), Constants.ItemName.HoverUpgradeTier1)
  val HoverUpgradeTier2: DeferredItem[item.UpgradeHover] = registerItem(new item.UpgradeHover(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.HoverUpgradeTier2)

  // 1.6
  val TradingUpgrade: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps.rarity(Rarity.UNCOMMON), "upgradetrading"), Constants.ItemName.TradingUpgrade)
  val MFU: DeferredItem[item.UpgradeMF] = registerItem(new item.UpgradeMF(defaultProps.rarity(Rarity.RARE)), Constants.ItemName.MFU)

  // 1.7.2
  val WirelessNetworkCardTier1: DeferredItem[item.WirelessNetworkCard] = registerItem(new item.WirelessNetworkCard(defaultProps, Tier.One), Constants.ItemName.WirelessNetworkCardTier1)
  val ComponentBusCreative: DeferredItem[item.ComponentBus] = registerItem(new item.ComponentBus(defaultProps.rarity(Rarity.EPIC), Tier.Five), Constants.ItemName.ComponentBusCreative)

  // 1.8
  val StickyPistonUpgrade: DeferredItem[Item] = registerItem(new item.BasicTieredItem(defaultProps, "upgradestickypiston"), Constants.ItemName.StickyPistonUpgrade)

  /////////////////////////////////////////////////////////////////
  // Storage media of all kinds.
  /////////////////////////////////////////////////////////////////
  val EEPROM: DeferredItem[item.EEPROM] = registerItem(new item.EEPROM(defaultProps), Constants.ItemName.EEPROM)
  val Floppy: DeferredItem[item.FloppyDisk] = registerItem(new item.FloppyDisk(defaultProps), Constants.ItemName.Floppy)
  val HDDTier1: DeferredItem[item.HardDiskDrive] = registerItem(new item.HardDiskDrive(defaultProps, Tier.One), Constants.ItemName.HDDTier1)
  val HDDTier2: DeferredItem[item.HardDiskDrive] = registerItem(new item.HardDiskDrive(defaultProps.rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.HDDTier2)
  val HDDTier3: DeferredItem[item.HardDiskDrive] = registerItem(new item.HardDiskDrive(defaultProps.rarity(Rarity.RARE), Tier.Three), Constants.ItemName.HDDTier3)
  val HDDTier4: DeferredItem[item.HardDiskDrive] = registerItem(new item.HardDiskDrive(defaultProps.rarity(OCRarity.LEGENDARY), Tier.Four), Constants.ItemName.HDDTier4)

  val SSDTier1: DeferredItem[item.SolidStateDrive] = registerItem(new item.SolidStateDrive(defaultProps.rarity(Rarity.UNCOMMON), Tier.One), Constants.ItemName.SSDTier1)
  val SSDTier2: DeferredItem[item.SolidStateDrive] = registerItem(new item.SolidStateDrive(defaultProps.rarity(Rarity.RARE), Tier.Two), Constants.ItemName.SSDTier2)
  val SSDTier3: DeferredItem[item.SolidStateDrive] = registerItem(new item.SolidStateDrive(defaultProps.rarity(OCRarity.LEGENDARY), Tier.Three), Constants.ItemName.SSDTier3)

  private def initPostStorage(): Unit = {
    val luaBios = {
      val code = new Array[Byte](4 * 1024)
      val count = OpenComputers.getClass.getResourceAsStream(Settings.scriptPath + "bios.lua").read(code)
      createEEPROM("EEPROM (Lua BIOS)", code.take(count), null, readonly = false)
    }
    registerStack(luaBios, Constants.ItemName.LuaBios)
  }

  /////////////////////////////////////////////////////////////////
  // Special purpose items that don't fit into any other category.
  /////////////////////////////////////////////////////////////////
  val Tablet: DeferredItem[item.Tablet] = registerItem(new item.Tablet(defaultProps.stacksTo(1)), Constants.ItemName.Tablet)
  val Drone: DeferredItem[item.Drone] = registerItem(new item.Drone(defaultProps), Constants.ItemName.Drone)
  val Present: DeferredItem[item.Present] = registerItem(new item.Present(defaultProps), Constants.ItemName.Present)

  def decorateCreativeTab(event: BuildCreativeModeTabContentsEvent, hasRedstoneCardT2: Boolean): Unit = {
    import Constants.{BlockName => B, ItemName => I}
    // Assembled devices are not usable without their component data. Their
    // configured creative variants are added explicitly below.
    val excluded = Set(B.Microcontroller, B.Print, B.Robot, I.Drone, I.Tablet)

    def accept(id: String, info: ItemInfo): Unit = {
      if (id != B.PowerConverter || !Settings.get.ignorePower) {
        val stack = info.createItemStack(1)
        if (!stack.isEmpty) event.accept(stack)
      }
    }

    // Block items first, then regular items — mirrors 1.16.5 registry order.
    // Items registered at mod-load time enter descriptors before blocks (which are
    // registered lazily during the BLOCKS event), so we partition explicitly.
    // Loot disk floppies (registered into descriptors via Items.registerStack in
    // Loot.createLootDisk) are excluded here because they're already added below via
    // Loot.disksForClient; including both caused duplicate ItemStack entries and crashed
    // BuildCreativeModeTabContentsEvent.
    for ((id, info) <- descriptors if info.block != null && !excluded.contains(id) && id != I.RedstoneCardTier2 && !Loot.lootDiskDescriptorIds.contains(id))
      accept(id, info)
    for ((id, info) <- descriptors if info.block == null && !excluded.contains(id) && id != I.RedstoneCardTier2 && !Loot.lootDiskDescriptorIds.contains(id))
      accept(id, info)

    event.accept(OCItems.createConfiguredDrone())
    event.accept(OCItems.createConfiguredMicrocontroller())
    event.accept(OCItems.createConfiguredRobot())
    event.accept(OCItems.createConfiguredTablet())

    Loot.disksForClient.foreach(event.accept)
    registeredItems.foreach(event.accept)

    if (hasRedstoneCardT2) {
      descriptors.get(Constants.ItemName.RedstoneCardTier2).foreach { info =>
        event.accept(info.createItemStack(1))
      }
    }
  }
}
