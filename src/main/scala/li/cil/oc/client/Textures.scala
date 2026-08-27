package li.cil.oc.client

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.{Constants, OpenComputers, Settings}
import li.cil.oc.common.{Slot, Tier}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.renderer.texture.{SimpleTexture, TextureAtlasSprite}
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.{ResourceManager, ResourceManagerReloadListener}
import net.minecraft.world.inventory.InventoryMenu
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent

import scala.collection.mutable

object Textures {
  object Font extends SimpleTextureBundle {
    val Aliased = L("chars_aliased")
    val AntiAliased = L("chars")

    override protected def basePath = "font/%s"
  }

  object GUI {
    private def L(name: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "textures/gui/" + name + ".png")

    val Background = L("background")
    val Borders = L("borders")
    val Computer = L("computer")
    val Database = L("database")
    val Database1 = L("database1")
    val Database2 = L("database2")
    val Disassembler = L("disassembler")
    val Drive = L("drive")
    val Drone = L("drone")
    val Manual = L("manual")
    val Nanomachines = L("nanomachines_power")
    val NanomachinesBar = L("nanomachines_power_bar")
    val Printer = L("printer")
    val Rack = L("rack")
    val Raid = L("raid")
    val Range = L("range")
    val Robot = L("robot")
    val RobotAssembler = L("robot_assembler")
    val RobotNoScreen = L("robot_noscreen")
    val Server = L("server")
    val Slot = L("slot")
    val Waypoint = L("waypoint")
  }

  object GUISprites {
    private def L(name: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, name)

    private def simpleButton(name: String): WidgetSprites = new WidgetSprites(L(name), L(name).withSuffix("_highlight"))

    val Bar: ResourceLocation = L("bar")
    val KeyboardMissing: ResourceLocation = L("keyboard_missing")
    val ManualMissingItem = L("manual_missing_item")
    val PrinterInk: ResourceLocation = L("printer_ink")
    val PrinterMaterial: ResourceLocation = L("printer_material")
    val PrinterProgress: ResourceLocation = L("printer_progress")
    val RobotSelection: ResourceLocation = L("robot_selection")
    val UpgradeTab: ResourceLocation = L("upgrade_tab")

    val ButtonDriveMode: WidgetSprites = new WidgetSprites(L("button_drive_mode"), L("button_drive_mode_disabled"), L("button_drive_mode_highlight"))
    val ButtonPower: WidgetSprites = new WidgetSprites(
      L("button_power_on"), L("button_power_off"), L("button_power_on_highlight"), L("button_power_off_highlight")
    )
    val ButtonRelay: WidgetSprites = simpleButton("button_relay")
    val ButtonRun: WidgetSprites = new WidgetSprites(L("button_run"), L("button_run_disabled"), L("button_run_highlight"))
    val ButtonScroll: WidgetSprites = simpleButton("button_scroll")
    val ManualHome: ResourceLocation = L("manual_home")
    val ManualTab: WidgetSprites = simpleButton("manual_tab")
  }

  object Icons extends SimpleTextureBundle {
    private val ForSlotType = Slot.All.map(name => name -> L(name)).toMap
    private val ForTier = Map(Tier.None -> L("na")) ++ (Tier.One to Tier.Four).map(tier => tier -> L("tier" + tier)).toMap

    def get(slotType: String) = ForSlotType.get(slotType).orNull

    def get(tier: Int) = ForTier.get(tier).orNull

    override protected def basePath = "icons/%s"
  }

  object Model extends SimpleTextureBundle {
    val UpgradeCrafting = L("crafting_upgrade")
    val UpgradeGenerator = L("generator_upgrade")
    val UpgradeInventory = L("inventory_upgrade")
    val HologramEffect = L("hologram_effect")
    val Drone = L("drone")
    val Robot = L("robot")
    val RobotProgressFlag = L("robot_progress_flag")
    val RobotLesbianFlag = L("robot_lesbian_flag")
    val RobotBisexualFlag = L("robot_bisexual_flag")
    val RobotPansexualFlag = L("robot_pansexual_flag")
    val RobotAsexualFlag = L("robot_asexual_flag")
    val RobotRainbowFlag = L("robot_rainbow_flag")
    val RobotTransFlag = L("robot_trans_flag")

    override protected def basePath = "model/%s"
  }

  object Item {
    val DroneItem = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "item/drone")
    val Robot = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "item/robot")
    private val LocationTerminalOn = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain, Constants.ItemName.Terminal + "_on")
    private val LocationTerminalOff = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain, Constants.ItemName.Terminal + "_off")
    val TerminalOn = new ModelResourceLocation(LocationTerminalOn, "inventory")
    val TerminalOff = new ModelResourceLocation(LocationTerminalOff, "inventory")
  }

  object Block {
    val AdapterOn = L("overlay/adapter_on")
    val AssemblerSideAssembling = L("overlay/assembler_side_assembling")
    val AssemblerSideOn = L("overlay/assembler_side_on")
    val AssemblerTopOn = L("overlay/assembler_top_on")
    val CaseFrontActivity = L("overlay/case_front_activity")
    val CaseFrontError = L("overlay/case_front_error")
    val CaseFrontOn = L("overlay/case_front_on")
    val ChargerFrontOn = L("overlay/charger_front_on")
    val ChargerSideOn = L("overlay/charger_side_on")
    val DisassemblerSideOn = L("overlay/disassembler_side_on")
    val DisassemblerTopOn = L("overlay/disassembler_top_on")
    val DiskDriveFrontActivity = L("overlay/diskdrive_front_activity")
    val GeolyzerTopOn = L("overlay/geolyzer_top_on")
    val MicrocontrollerFrontLight = L("overlay/microcontroller_front_light")
    val MicrocontrollerFrontOn = L("overlay/microcontroller_front_on")
    val MicrocontrollerFrontError = L("overlay/microcontroller_front_error")
    val NetSplitterOn = L("overlay/netsplitter_on")
    val PowerDistributorSideOn = L("overlay/powerdistributor_side_on")
    val PowerDistributorTopOn = L("overlay/powerdistributor_top_on")
    val RackDiskDrive = L("rack_disk_drive")
    val RackDiskDriveActivity = L("overlay/rack_disk_drive_activity")
    val RackCapacitor = L("rack_capacitor")
    val RackCapacitorOn = L("overlay/rack_capacitor_on")
    val RackServer = L("rack_server")
    val RackServerActivity = L("overlay/rack_server_activity")
    val RackServerOn = L("overlay/rack_server_on")
    val RackServerError = L("overlay/rack_server_error")
    val RackServerNetworkActivity = L("overlay/rack_server_network_activity")
    val RackTerminalServer = L("rack_terminal_server")
    val RackTerminalServerOn = L("overlay/rack_terminal_server_on")
    val RackTerminalServerPresence = L("overlay/rack_terminal_server_presence")
    val RackKVM = L("rack_kvm")
    val RackKVMPresence = L("overlay/rack_kvm_presence")
    val RaidFrontActivity = L("overlay/raid_front_activity")
    val RaidFrontError = L("overlay/raid_front_error")
    val ScreenUpIndicator = L("overlay/screen_up_indicator")
    val SwitchSideOn = L("overlay/switch_side_on")
    val TransposerOn = L("overlay/transposer_on")
    val GenericTop = L("generic_top")
    val NetSplitterSide = L("netsplitter_side")
    val NetSplitterTop = L("netsplitter_top")
    val RackFront = L("rack_front")
    val RackSide = L("rack_side")

    object Screen {
      val Single = Array(
        L("screen/b"), L("screen/b"), L("screen/b2"),
        L("screen/b2"), L("screen/b2"), L("screen/b2")
      )
      val SingleFront = Array(L("screen/f"), L("screen/f2"))
      val Horizontal = Array(Array(Array(L("screen/bht"),L("screen/bhb"),L("screen/bht2"),L("screen/bht2"),L("screen/b2"),L("screen/b2")),Array(L("screen/bhm"),L("screen/bhm"),L("screen/bhm2"),L("screen/bhm2"),L("screen/b"),L("screen/b")),Array(L("screen/bhb"),L("screen/bht"),L("screen/bhb2"),L("screen/bhb2"),L("screen/b2"),L("screen/b2"))),Array(Array(L("screen/bhb2"),L("screen/bht2"),L("screen/bht"),L("screen/bhb"),L("screen/b2"),L("screen/b2")),Array(L("screen/bhm2"),L("screen/bhm2"),L("screen/bhm"),L("screen/bhm"),L("screen/b"),L("screen/b")),Array(L("screen/bht2"),L("screen/bhb2"),L("screen/bhb"),L("screen/bht"),L("screen/b2"),L("screen/b2"))))
      val HorizontalFront = Array(Array(L("screen/fhb2"),L("screen/fhm2"),L("screen/fht2")),Array(L("screen/fhb"),L("screen/fhm"),L("screen/fht")))
      val Vertical = Array(Array(Array(L("screen/b"),L("screen/b"),L("screen/bvt"),L("screen/bvt"),L("screen/bvt"),L("screen/bvt")),Array(L("screen/b"),L("screen/b"),L("screen/bvm"),L("screen/bvm"),L("screen/bvm"),L("screen/bvm")),Array(L("screen/b"),L("screen/b"),L("screen/bvb2"),L("screen/bvb2"),L("screen/bvb2"),L("screen/bvb2"))),Array(Array(L("screen/b2"),L("screen/b2"),L("screen/bvt"),L("screen/bvt"),L("screen/bht2"),L("screen/bhb2")),Array(L("screen/b"),L("screen/b"),L("screen/bvm"),L("screen/bvm"),L("screen/bhm2"),L("screen/bhm2")),Array(L("screen/b2"),L("screen/b2"),L("screen/bvb"),L("screen/bvb"),L("screen/bhb2"),L("screen/bht2"))))
      val VerticalFront = Array(Array(L("screen/fvt"),L("screen/fvm"),L("screen/fvb2")),Array(L("screen/fvt"),L("screen/fvm"),L("screen/fvb")))
      val Multi = Array(Array(Array(Array(L("screen/bht"),L("screen/bhb"),L("screen/btl"),L("screen/btr"),L("screen/bvb"),L("screen/bvt")),Array(L("screen/bhm"),L("screen/bhm"),L("screen/btm"),L("screen/btm"),L("screen/b"),L("screen/b")),Array(L("screen/bhb"),L("screen/bht"),L("screen/btr"),L("screen/btl"),L("screen/bvt"),L("screen/bvb"))),Array(Array(L("screen/b"),L("screen/b"),L("screen/bml"),L("screen/bmr"),L("screen/bvm"),L("screen/bvm")),Array(L("screen/b"),L("screen/b"),L("screen/bmm"),L("screen/bmm"),L("screen/b"),L("screen/b")),Array(L("screen/b"),L("screen/b"),L("screen/bmr"),L("screen/bml"),L("screen/bvm"),L("screen/bvt"))),Array(Array(L("screen/bht"),L("screen/bhb"),L("screen/bbl2"),L("screen/bbr2"),L("screen/bvt"),L("screen/bvb2")),Array(L("screen/bhm"),L("screen/bhm"),L("screen/bbm2"),L("screen/bbm2"),L("screen/b"),L("screen/b")),Array(L("screen/bhb"),L("screen/bht"),L("screen/bbr2"),L("screen/bbl2"),L("screen/bvb2"),L("screen/bvt")))),Array(Array(Array(L("screen/bhb2"),L("screen/bht2"),L("screen/btl"),L("screen/btr"),L("screen/bht2"),L("screen/bhb2")),Array(L("screen/bhm2"),L("screen/bhm2"),L("screen/btm"),L("screen/btm"),L("screen/b"),L("screen/b")),Array(L("screen/bht2"),L("screen/bhb2"),L("screen/btr"),L("screen/btl"),L("screen/bht2"),L("screen/bhb2"))),Array(Array(L("screen/b"),L("screen/b"),L("screen/bml"),L("screen/bml"),L("screen/bhm2"),L("screen/bhm2")),Array(L("screen/b"),L("screen/b"),L("screen/bmm"),L("screen/bmm"),L("screen/b"),L("screen/b")),Array(L("screen/b"),L("screen/b"),L("screen/bmr"),L("screen/bmr"),L("screen/bhm2"),L("screen/bhm2"))),Array(Array(L("screen/bhb2"),L("screen/bht2"),L("screen/bbl"),L("screen/bbr"),L("screen/bhb2"),L("screen/bht2")),Array(L("screen/bhm2"),L("screen/bhm2"),L("screen/bbm"),L("screen/bbm"),L("screen/b"),L("screen/b")),Array(L("screen/bht2"),L("screen/bhb2"),L("screen/bbr"),L("screen/bbl"),L("screen/bhb2"),L("screen/bht2")))))
      val MultiFront = Array(Array(Array(L("screen/ftr"),L("screen/ftm"),L("screen/ftl")),Array(L("screen/fmr"),L("screen/fmm"),L("screen/fml")),Array(L("screen/fbr2"),L("screen/fbm2"),L("screen/fbl2"))),Array(Array(L("screen/ftr"),L("screen/ftm"),L("screen/ftl")),Array(L("screen/fmr"),L("screen/fmm"),L("screen/fml")),Array(L("screen/fbr"),L("screen/fbm"),L("screen/fbl"))))

      private[Block] def makeSureThisIsInitialized(): Unit = {}
    }

    def bind(): Unit = Textures.bind(InventoryMenu.BLOCK_ATLAS)

    Screen.makeSureThisIsInitialized()

    private def L(name: String) = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, s"block/$name")
  }

  def getSprite(location: ResourceLocation): TextureAtlasSprite =
    Minecraft.getInstance.getModelManager.getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(location)

  def bind(location: ResourceLocation): Unit = {
    if (location != null) {
      RenderSystem.setShaderTexture(0, location)
    } else {
      RenderSystem.setShaderTexture(0, 0)
    }
  }

  @SubscribeEvent
  def onRegisterReloadListeners(e: RegisterClientReloadListenersEvent): Unit = {
    e.registerReloadListener(new ResourceManagerReloadListener {
      override def onResourceManagerReload(manager: ResourceManager): Unit = {
        val tm = Minecraft.getInstance.getTextureManager
        def register(bundle: SimpleTextureBundle): Unit = {
          bundle.locations.foreach { loc =>
            tm.register(loc, new SimpleTexture(ResourceLocation.fromNamespaceAndPath(loc.getNamespace, s"textures/${loc.getPath}.png")))
          }
        }
        register(Font)
        register(Icons)
        register(Model)
      }
    })
  }

  abstract class SimpleTextureBundle {
    private[Textures] val locations = mutable.ArrayBuffer.empty[ResourceLocation]

    protected def L(name: String, load: Boolean = true): ResourceLocation = {
      val location = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, String.format(basePath, name))
      if (load) locations += location
      location
    }

    protected def basePath: String
  }
}
