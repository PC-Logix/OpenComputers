package li.cil.oc.common.init

import li.cil.oc.common.Tier
import li.cil.oc.common.block._
import li.cil.oc.common.init.{OCItems => Items}
import li.cil.oc.util.{Rarity => OCRarity}
import li.cil.oc.{Constants, Settings}
import net.minecraft.world.item.{Item, Rarity}
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.material.MapColor
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.{DeferredBlock, DeferredRegister}

object OCBlocks {
  private val BLOCKS: DeferredRegister.Blocks = DeferredRegister.createBlocks(Settings.resourceDomain)

  private def defaultProps = Properties.of().mapColor(MapColor.METAL).strength(2, 5)

  private def defaultItemProps = new Item.Properties()

  val Adapter: DeferredBlock[Adapter] = BLOCKS.register(Constants.BlockName.Adapter, () => Items.registerBlock(new Adapter(defaultProps), Constants.BlockName.Adapter, defaultItemProps))
  val Assembler: DeferredBlock[Assembler] = BLOCKS.register(Constants.BlockName.Assembler, () => Items.registerBlock(new Assembler(defaultProps), Constants.BlockName.Assembler, defaultItemProps))
  val Cable: DeferredBlock[Cable] = BLOCKS.register(Constants.BlockName.Cable, () => Items.registerBlock(new Cable(defaultProps), Constants.BlockName.Cable, defaultItemProps))
  val Capacitor: DeferredBlock[Capacitor] = BLOCKS.register(Constants.BlockName.Capacitor, () => Items.registerBlock(new Capacitor(defaultProps), Constants.BlockName.Capacitor, defaultItemProps))
  val CaseTier1: DeferredBlock[Case] = BLOCKS.register(Constants.BlockName.CaseTier1, () => Items.registerBlock(new Case(defaultProps, Tier.One), Constants.BlockName.CaseTier1, defaultItemProps))
  val CaseTier2: DeferredBlock[Case] = BLOCKS.register(Constants.BlockName.CaseTier2, () => Items.registerBlock(new Case(defaultProps, Tier.Two), Constants.BlockName.CaseTier2, defaultItemProps.rarity(Rarity.UNCOMMON)))
  val CaseTier3: DeferredBlock[Case] = BLOCKS.register(Constants.BlockName.CaseTier3, () => Items.registerBlock(new Case(defaultProps, Tier.Three), Constants.BlockName.CaseTier3, defaultItemProps.rarity(Rarity.RARE)))
  val CaseTier4: DeferredBlock[Case] = BLOCKS.register(Constants.BlockName.CaseTier4, () => Items.registerBlock(new Case(defaultProps, Tier.Four), Constants.BlockName.CaseTier4, defaultItemProps.rarity(OCRarity.LEGENDARY)))
  val ChameliumBlock: DeferredBlock[ChameliumBlock] = BLOCKS.register(Constants.BlockName.ChameliumBlock, () => Items.registerBlock(new ChameliumBlock(Properties.of().mapColor(MapColor.STONE).strength(2, 5)), Constants.BlockName.ChameliumBlock, defaultItemProps))
  val Charger: DeferredBlock[Charger] = BLOCKS.register(Constants.BlockName.Charger, () => Items.registerBlock(new Charger(defaultProps), Constants.BlockName.Charger, defaultItemProps))
  val Disassembler: DeferredBlock[Disassembler] = BLOCKS.register(Constants.BlockName.Disassembler, () => Items.registerBlock(new Disassembler(defaultProps), Constants.BlockName.Disassembler, defaultItemProps))
  val DiskDrive: DeferredBlock[DiskDrive] = BLOCKS.register(Constants.BlockName.DiskDrive, () => Items.registerBlock(new DiskDrive(defaultProps), Constants.BlockName.DiskDrive, defaultItemProps))
  val Geolyzer: DeferredBlock[Geolyzer] = BLOCKS.register(Constants.BlockName.Geolyzer, () => Items.registerBlock(new Geolyzer(defaultProps), Constants.BlockName.Geolyzer, defaultItemProps))
  val HologramTier1: DeferredBlock[Hologram] = BLOCKS.register(Constants.BlockName.HologramTier1, () => Items.registerBlock(new Hologram(defaultProps, Tier.One), Constants.BlockName.HologramTier1, defaultItemProps))
  val HologramTier2: DeferredBlock[Hologram] = BLOCKS.register(Constants.BlockName.HologramTier2, () => Items.registerBlock(new Hologram(defaultProps, Tier.Two), Constants.BlockName.HologramTier2, defaultItemProps.rarity(Rarity.UNCOMMON)))
  val HologramTier3: DeferredBlock[Hologram] = BLOCKS.register(Constants.BlockName.HologramTier3, () => Items.registerBlock(new Hologram(defaultProps, Tier.Three), Constants.BlockName.HologramTier3, defaultItemProps.rarity(Rarity.RARE)))
  val HoloScreenTier1: DeferredBlock[HoloScreen] = BLOCKS.register(Constants.BlockName.HoloScreenTier1, () => Items.registerBlock(new HoloScreen(defaultProps.noOcclusion, Tier.One), Constants.BlockName.HoloScreenTier1, defaultItemProps))
  val HoloScreenTier2: DeferredBlock[HoloScreen] = BLOCKS.register(Constants.BlockName.HoloScreenTier2, () => Items.registerBlock(new HoloScreen(defaultProps.noOcclusion, Tier.Two), Constants.BlockName.HoloScreenTier2, defaultItemProps.rarity(Rarity.UNCOMMON)))
  val HoloScreenTier3: DeferredBlock[HoloScreen] = BLOCKS.register(Constants.BlockName.HoloScreenTier3, () => Items.registerBlock(new HoloScreen(defaultProps.noOcclusion, Tier.Three), Constants.BlockName.HoloScreenTier3, defaultItemProps.rarity(Rarity.RARE)))
  val HoloScreenTier4: DeferredBlock[HoloScreen] = BLOCKS.register(Constants.BlockName.HoloScreenTier4, () => Items.registerBlock(new HoloScreen(defaultProps.noOcclusion, Tier.Four), Constants.BlockName.HoloScreenTier4, defaultItemProps.rarity(OCRarity.LEGENDARY)))
  val Keyboard: DeferredBlock[Keyboard] = BLOCKS.register(Constants.BlockName.Keyboard, () => Items.registerBlock(new Keyboard(Properties.of().mapColor(MapColor.STONE).strength(2, 5).noOcclusion), Constants.BlockName.Keyboard, defaultItemProps))
  val MotionSensor: DeferredBlock[MotionSensor] = BLOCKS.register(Constants.BlockName.MotionSensor, () => Items.registerBlock(new MotionSensor(defaultProps), Constants.BlockName.MotionSensor, defaultItemProps))
  val PowerConverter: DeferredBlock[PowerConverter] = BLOCKS.register(Constants.BlockName.PowerConverter, () => Items.registerBlock(new PowerConverter(defaultProps), Constants.BlockName.PowerConverter, defaultItemProps))
  val PowerDistributor: DeferredBlock[PowerDistributor] = BLOCKS.register(Constants.BlockName.PowerDistributor, () => Items.registerBlock(new PowerDistributor(defaultProps), Constants.BlockName.PowerDistributor, defaultItemProps))
  val Printer: DeferredBlock[Printer] = BLOCKS.register(Constants.BlockName.Printer, () => Items.registerBlock(new Printer(defaultProps), Constants.BlockName.Printer, defaultItemProps))
  val Raid: DeferredBlock[Raid] = BLOCKS.register(Constants.BlockName.Raid, () => Items.registerBlock(new Raid(defaultProps), Constants.BlockName.Raid, defaultItemProps))
  val Redstone: DeferredBlock[Redstone] = BLOCKS.register(Constants.BlockName.Redstone, () => Items.registerBlock(new Redstone(defaultProps), Constants.BlockName.Redstone, defaultItemProps))
  val Relay: DeferredBlock[Relay] = BLOCKS.register(Constants.BlockName.Relay, () => Items.registerBlock(new Relay(defaultProps), Constants.BlockName.Relay, defaultItemProps))
  val ScreenTier1: DeferredBlock[Screen] = BLOCKS.register(Constants.BlockName.ScreenTier1, () => Items.registerBlock(new Screen(defaultProps, Tier.One), Constants.BlockName.ScreenTier1, defaultItemProps))
  val ScreenTier2: DeferredBlock[Screen] = BLOCKS.register(Constants.BlockName.ScreenTier2, () => Items.registerBlock(new Screen(defaultProps, Tier.Two), Constants.BlockName.ScreenTier2, defaultItemProps.rarity(Rarity.UNCOMMON)))
  val ScreenTier3: DeferredBlock[Screen] = BLOCKS.register(Constants.BlockName.ScreenTier3, () => Items.registerBlock(new Screen(defaultProps, Tier.Three), Constants.BlockName.ScreenTier3, defaultItemProps.rarity(Rarity.RARE)))
  val ScreenTier4: DeferredBlock[Screen] = BLOCKS.register(Constants.BlockName.ScreenTier4, () => Items.registerBlock(new Screen(defaultProps, Tier.Four), Constants.BlockName.ScreenTier4, defaultItemProps.rarity(OCRarity.LEGENDARY)))
  val FlatScreenBackTier1: DeferredBlock[FlatScreen] = BLOCKS.register(Constants.BlockName.FlatScreenBackTier1, () => Items.registerBlock(new FlatScreen(defaultProps.noOcclusion, Tier.One, true), Constants.BlockName.FlatScreenBackTier1, defaultItemProps))
  val FlatScreenBackTier2: DeferredBlock[FlatScreen] = BLOCKS.register(Constants.BlockName.FlatScreenBackTier2, () => Items.registerBlock(new FlatScreen(defaultProps.noOcclusion, Tier.Two, true), Constants.BlockName.FlatScreenBackTier2, defaultItemProps.rarity(Rarity.UNCOMMON)))
  val FlatScreenBackTier3: DeferredBlock[FlatScreen] = BLOCKS.register(Constants.BlockName.FlatScreenBackTier3, () => Items.registerBlock(new FlatScreen(defaultProps.noOcclusion, Tier.Three, true), Constants.BlockName.FlatScreenBackTier3, defaultItemProps.rarity(Rarity.RARE)))
  val FlatScreenBackTier4: DeferredBlock[FlatScreen] = BLOCKS.register(Constants.BlockName.FlatScreenBackTier4, () => Items.registerBlock(new FlatScreen(defaultProps.noOcclusion, Tier.Four, true), Constants.BlockName.FlatScreenBackTier4, defaultItemProps.rarity(OCRarity.LEGENDARY)))
  val FlatScreenFrontTier1: DeferredBlock[FlatScreen] = BLOCKS.register(Constants.BlockName.FlatScreenFrontTier1, () => Items.registerBlock(new FlatScreen(defaultProps.noOcclusion, Tier.One, false), Constants.BlockName.FlatScreenFrontTier1, defaultItemProps))
  val FlatScreenFrontTier2: DeferredBlock[FlatScreen] = BLOCKS.register(Constants.BlockName.FlatScreenFrontTier2, () => Items.registerBlock(new FlatScreen(defaultProps.noOcclusion, Tier.Two, false), Constants.BlockName.FlatScreenFrontTier2, defaultItemProps.rarity(Rarity.UNCOMMON)))
  val FlatScreenFrontTier3: DeferredBlock[FlatScreen] = BLOCKS.register(Constants.BlockName.FlatScreenFrontTier3, () => Items.registerBlock(new FlatScreen(defaultProps.noOcclusion, Tier.Three, false), Constants.BlockName.FlatScreenFrontTier3, defaultItemProps.rarity(Rarity.RARE)))
  val FlatScreenFrontTier4: DeferredBlock[FlatScreen] = BLOCKS.register(Constants.BlockName.FlatScreenFrontTier4, () => Items.registerBlock(new FlatScreen(defaultProps.noOcclusion, Tier.Four, false), Constants.BlockName.FlatScreenFrontTier4, defaultItemProps.rarity(OCRarity.LEGENDARY)))
  val Rack: DeferredBlock[Rack] = BLOCKS.register(Constants.BlockName.Rack, () => Items.registerBlock(new Rack(defaultProps), Constants.BlockName.Rack, defaultItemProps))
  val Waypoint: DeferredBlock[Waypoint] = BLOCKS.register(Constants.BlockName.Waypoint, () => Items.registerBlock(new Waypoint(defaultProps), Constants.BlockName.Waypoint, defaultItemProps))

  val CaseCreative: DeferredBlock[Case] = BLOCKS.register(Constants.BlockName.CaseCreative, () => Items.registerBlock(new Case(defaultProps, Tier.Five), Constants.BlockName.CaseCreative, defaultItemProps.rarity(Rarity.EPIC)))
  val Microcontroller: DeferredBlock[Microcontroller] = BLOCKS.register(Constants.BlockName.Microcontroller, () => Items.registerBlock(new Microcontroller(defaultProps), Constants.BlockName.Microcontroller, new Item.Properties()))
  val Print: DeferredBlock[Print] = BLOCKS.register(Constants.BlockName.Print, () => Items.registerBlock(new Print(Properties.of().mapColor(MapColor.METAL).strength(1, 5).noOcclusion.dynamicShape), Constants.BlockName.Print, new Item.Properties()))
  val RobotAfterimage: DeferredBlock[RobotAfterimage] = BLOCKS.register(Constants.BlockName.RobotAfterimage, () => Items.registerBlockOnly(new RobotAfterimage(Properties.of().mapColor(MapColor.NONE).noCollission.instabreak.noOcclusion.dynamicShape), Constants.BlockName.RobotAfterimage))
  val Robot: DeferredBlock[RobotProxy] = BLOCKS.register(Constants.BlockName.Robot, () => Items.registerBlock(new RobotProxy(defaultProps.noOcclusion.dynamicShape), Constants.BlockName.Robot, new Item.Properties()))

  // v1.5.10
  val Endstone: DeferredBlock[FakeEndstone] = BLOCKS.register(Constants.BlockName.Endstone, () => Items.registerBlock(new FakeEndstone(Properties.of().mapColor(MapColor.STONE).strength(3, 15)), Constants.BlockName.Endstone, defaultItemProps))

  // v1.5.14
  val NetSplitter: DeferredBlock[NetSplitter] = BLOCKS.register(Constants.BlockName.NetSplitter, () => Items.registerBlock(new NetSplitter(defaultProps), Constants.BlockName.NetSplitter, defaultItemProps))

  // v1.5.16
  val Transposer: DeferredBlock[Transposer] = BLOCKS.register(Constants.BlockName.Transposer, () => Items.registerBlock(new Transposer(defaultProps), Constants.BlockName.Transposer, defaultItemProps))

  // v1.7.2
  val CarpetedCapacitor: DeferredBlock[CarpetedCapacitor] = BLOCKS.register(Constants.BlockName.CarpetedCapacitor, () => Items.registerBlock(new CarpetedCapacitor(defaultProps), Constants.BlockName.CarpetedCapacitor, defaultItemProps))

  // Computronics content port. External-mod integrations are intentionally
  // excluded; the native block-entity behavior is registered below.
  private def computronicsProps = Properties.of().mapColor(MapColor.METAL).strength(2, 5)
  val ComputronicsIronNote: DeferredBlock[ComputronicsBlock] = BLOCKS.register(Constants.BlockName.ComputronicsIronNote, () => Items.registerBlock(new ComputronicsBlock(computronicsProps, Constants.BlockName.ComputronicsIronNote), Constants.BlockName.ComputronicsIronNote, defaultItemProps))
  val ComputronicsAudioCable: DeferredBlock[ComputronicsBlock] = BLOCKS.register(Constants.BlockName.ComputronicsAudioCable, () => Items.registerBlock(new ComputronicsBlock(computronicsProps, Constants.BlockName.ComputronicsAudioCable), Constants.BlockName.ComputronicsAudioCable, defaultItemProps))
  val ComputronicsSpeaker: DeferredBlock[ComputronicsBlock] = BLOCKS.register(Constants.BlockName.ComputronicsSpeaker, () => Items.registerBlock(new ComputronicsBlock(computronicsProps, Constants.BlockName.ComputronicsSpeaker), Constants.BlockName.ComputronicsSpeaker, defaultItemProps))
  val ComputronicsTapeReader: DeferredBlock[ComputronicsBlock] = BLOCKS.register(Constants.BlockName.ComputronicsTapeReader, () => Items.registerBlock(new ComputronicsBlock(computronicsProps, Constants.BlockName.ComputronicsTapeReader), Constants.BlockName.ComputronicsTapeReader, defaultItemProps))
  val ComputronicsCamera: DeferredBlock[ComputronicsBlock] = BLOCKS.register(Constants.BlockName.ComputronicsCamera, () => Items.registerBlock(new ComputronicsBlock(computronicsProps, Constants.BlockName.ComputronicsCamera), Constants.BlockName.ComputronicsCamera, defaultItemProps))
  val ComputronicsChatBox: DeferredBlock[ComputronicsBlock] = BLOCKS.register(Constants.BlockName.ComputronicsChatBox, () => Items.registerBlock(new ComputronicsBlock(computronicsProps, Constants.BlockName.ComputronicsChatBox), Constants.BlockName.ComputronicsChatBox, defaultItemProps))
  val ComputronicsCipher: DeferredBlock[ComputronicsBlock] = BLOCKS.register(Constants.BlockName.ComputronicsCipher, () => Items.registerBlock(new ComputronicsBlock(computronicsProps, Constants.BlockName.ComputronicsCipher), Constants.BlockName.ComputronicsCipher, defaultItemProps))
  val ComputronicsCipherAdvanced: DeferredBlock[ComputronicsBlock] = BLOCKS.register(Constants.BlockName.ComputronicsCipherAdvanced, () => Items.registerBlock(new ComputronicsBlock(computronicsProps, Constants.BlockName.ComputronicsCipherAdvanced), Constants.BlockName.ComputronicsCipherAdvanced, defaultItemProps))
  val ComputronicsRadar: DeferredBlock[ComputronicsBlock] = BLOCKS.register(Constants.BlockName.ComputronicsRadar, () => Items.registerBlock(new ComputronicsBlock(computronicsProps, Constants.BlockName.ComputronicsRadar), Constants.BlockName.ComputronicsRadar, defaultItemProps))
  val ComputronicsColorfulLamp: DeferredBlock[ComputronicsBlock] = BLOCKS.register(Constants.BlockName.ComputronicsColorfulLamp, () => Items.registerBlock(new ComputronicsBlock(computronicsProps, Constants.BlockName.ComputronicsColorfulLamp), Constants.BlockName.ComputronicsColorfulLamp, defaultItemProps))
  val ComputronicsSpeechBox: DeferredBlock[ComputronicsBlock] = BLOCKS.register(Constants.BlockName.ComputronicsSpeechBox, () => Items.registerBlock(new ComputronicsBlock(computronicsProps, Constants.BlockName.ComputronicsSpeechBox), Constants.BlockName.ComputronicsSpeechBox, defaultItemProps))

  def init(bus: IEventBus): Unit = {
    BLOCKS.register(bus)
  }
}
