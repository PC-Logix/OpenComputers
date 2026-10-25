package li.cil.oc.common.init;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.common.Tier;
import li.cil.oc.common.block.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Deferred block registrations and their creative-tab entries. */
public final class OCBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Settings.resourceDomain());
    private OCBlocks() { }

    private static BlockBehaviour.Properties defaultProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2, 5);
    }
    private static Item.Properties defaultItemProps() { return new Item.Properties(); }
    private static Item.Properties rare(Rarity rarity) { return defaultItemProps().rarity(rarity); }
    private static Item.Properties legendary() { return defaultItemProps().rarity(li.cil.oc.util.Rarity.LEGENDARY); }

    private static <T extends Block> DeferredBlock<T> register(String name, Supplier<T> factory,
                                                                Item.Properties item, String creativeTab) {
        return BLOCKS.register(name, () -> OCItemsRegistration.registerBlock(factory.get(), name, item, creativeTab));
    }
    private static <T extends Block> DeferredBlock<T> registerOnly(String name, Supplier<T> factory) {
        return BLOCKS.register(name, () -> OCItemsRegistration.registerBlockOnly(factory.get(), name));
    }

    public static final DeferredBlock<Adapter> Adapter = register(Constants.BlockName$.MODULE$.Adapter(), () -> new Adapter(defaultProps()), defaultItemProps(), "25_components");
    public static final DeferredBlock<Assembler> Assembler = register(Constants.BlockName$.MODULE$.Assembler(), () -> new Assembler(defaultProps()), defaultItemProps(), "49_tools");
    public static final DeferredBlock<Cable> Cable = register(Constants.BlockName$.MODULE$.Cable(), () -> new Cable(defaultProps()), defaultItemProps(), "10_networking");
    public static final DeferredBlock<Capacitor> Capacitor = register(Constants.BlockName$.MODULE$.Capacitor(), () -> new Capacitor(defaultProps()), defaultItemProps(), "10_networking");
    public static final DeferredBlock<Case> CaseTier1 = register(Constants.BlockName$.MODULE$.CaseTier1(), () -> new Case(defaultProps(), Tier.One), defaultItemProps(), "25_components");
    public static final DeferredBlock<Case> CaseTier2 = register(Constants.BlockName$.MODULE$.CaseTier2(), () -> new Case(defaultProps(), Tier.Two), rare(Rarity.UNCOMMON), "25_components");
    public static final DeferredBlock<Case> CaseTier3 = register(Constants.BlockName$.MODULE$.CaseTier3(), () -> new Case(defaultProps(), Tier.Three), rare(Rarity.RARE), "25_components");
    public static final DeferredBlock<Case> CaseTier4 = register(Constants.BlockName$.MODULE$.CaseTier4(), () -> new Case(defaultProps(), Tier.Four), legendary(), "25_components");
    public static final DeferredBlock<Case> CaseCreative = register(Constants.BlockName$.MODULE$.CaseCreative(), () -> new Case(defaultProps(), Tier.Five), rare(Rarity.EPIC), "25_components");
    public static final DeferredBlock<ChameliumBlock> ChameliumBlock = register(Constants.BlockName$.MODULE$.ChameliumBlock(), () -> new ChameliumBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2, 5)), defaultItemProps(), "50_materials");
    public static final DeferredBlock<Charger> Charger = register(Constants.BlockName$.MODULE$.Charger(), () -> new Charger(defaultProps()), defaultItemProps(), "10_networking");
    public static final DeferredBlock<Disassembler> Disassembler = register(Constants.BlockName$.MODULE$.Disassembler(), () -> new Disassembler(defaultProps()), defaultItemProps(), "49_tools");
    public static final DeferredBlock<DiskDrive> DiskDrive = register(Constants.BlockName$.MODULE$.DiskDrive(), () -> new DiskDrive(defaultProps()), defaultItemProps(), "25_components");
    public static final DeferredBlock<Geolyzer> Geolyzer = register(Constants.BlockName$.MODULE$.Geolyzer(), () -> new Geolyzer(defaultProps()), defaultItemProps(), "25_components");
    public static final DeferredBlock<Hologram> HologramTier1 = register(Constants.BlockName$.MODULE$.HologramTier1(), () -> new Hologram(defaultProps(), Tier.One), defaultItemProps(), "25_components");
    public static final DeferredBlock<Hologram> HologramTier2 = register(Constants.BlockName$.MODULE$.HologramTier2(), () -> new Hologram(defaultProps(), Tier.Two), rare(Rarity.UNCOMMON), "25_components");
    public static final DeferredBlock<Hologram> HologramTier3 = register(Constants.BlockName$.MODULE$.HologramTier3(), () -> new Hologram(defaultProps(), Tier.Three), rare(Rarity.RARE), "25_components");
    public static final DeferredBlock<Projector> Projector = register(Constants.BlockName$.MODULE$.Projector(), () -> new Projector(defaultProps().noOcclusion()), defaultItemProps(), "25_components");
    public static final DeferredBlock<HoloScreen> HoloScreenTier1 = register(Constants.BlockName$.MODULE$.HoloScreenTier1(), () -> new HoloScreen(defaultProps().noOcclusion(), Tier.One), defaultItemProps(), "25_components");
    public static final DeferredBlock<HoloScreen> HoloScreenTier2 = register(Constants.BlockName$.MODULE$.HoloScreenTier2(), () -> new HoloScreen(defaultProps().noOcclusion(), Tier.Two), rare(Rarity.UNCOMMON), "25_components");
    public static final DeferredBlock<HoloScreen> HoloScreenTier3 = register(Constants.BlockName$.MODULE$.HoloScreenTier3(), () -> new HoloScreen(defaultProps().noOcclusion(), Tier.Three), rare(Rarity.RARE), "25_components");
    public static final DeferredBlock<HoloScreen> HoloScreenTier4 = register(Constants.BlockName$.MODULE$.HoloScreenTier4(), () -> new HoloScreen(defaultProps().noOcclusion(), Tier.Four), legendary(), "25_components");
    public static final DeferredBlock<Keyboard> Keyboard = register(Constants.BlockName$.MODULE$.Keyboard(), () -> new Keyboard(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2, 5).noOcclusion()), defaultItemProps(), "25_components");
    public static final DeferredBlock<MotionSensor> MotionSensor = register(Constants.BlockName$.MODULE$.MotionSensor(), () -> new MotionSensor(defaultProps()), defaultItemProps(), "25_components");
    public static final DeferredBlock<PowerConverter> PowerConverter = register(Constants.BlockName$.MODULE$.PowerConverter(), () -> new PowerConverter(defaultProps()), defaultItemProps(), "10_networking");
    public static final DeferredBlock<PowerDistributor> PowerDistributor = register(Constants.BlockName$.MODULE$.PowerDistributor(), () -> new PowerDistributor(defaultProps()), defaultItemProps(), "10_networking");
    public static final DeferredBlock<Printer> Printer = register(Constants.BlockName$.MODULE$.Printer(), () -> new Printer(defaultProps()), defaultItemProps(), "25_components");
    public static final DeferredBlock<Raid> Raid = register(Constants.BlockName$.MODULE$.Raid(), () -> new Raid(defaultProps()), defaultItemProps(), "25_components");
    public static final DeferredBlock<Redstone> Redstone = register(Constants.BlockName$.MODULE$.Redstone(), () -> new Redstone(defaultProps()), defaultItemProps(), "25_components");
    public static final DeferredBlock<Relay> Relay = register(Constants.BlockName$.MODULE$.Relay(), () -> new Relay(defaultProps()), defaultItemProps(), "10_networking");
    public static final DeferredBlock<Screen> ScreenTier1 = register(Constants.BlockName$.MODULE$.ScreenTier1(), () -> new Screen(defaultProps(), Tier.One), defaultItemProps(), "25_components");
    public static final DeferredBlock<Screen> ScreenTier2 = register(Constants.BlockName$.MODULE$.ScreenTier2(), () -> new Screen(defaultProps(), Tier.Two), rare(Rarity.UNCOMMON), "25_components");
    public static final DeferredBlock<Screen> ScreenTier3 = register(Constants.BlockName$.MODULE$.ScreenTier3(), () -> new Screen(defaultProps(), Tier.Three), rare(Rarity.RARE), "25_components");
    public static final DeferredBlock<Screen> ScreenTier4 = register(Constants.BlockName$.MODULE$.ScreenTier4(), () -> new Screen(defaultProps(), Tier.Four), legendary(), "25_components");
    public static final DeferredBlock<FlatScreen> FlatScreenBackTier1 = register(Constants.BlockName$.MODULE$.FlatScreenBackTier1(), () -> new FlatScreen(defaultProps().noOcclusion(), Tier.One, true), defaultItemProps(), "25_components");
    public static final DeferredBlock<FlatScreen> FlatScreenBackTier2 = register(Constants.BlockName$.MODULE$.FlatScreenBackTier2(), () -> new FlatScreen(defaultProps().noOcclusion(), Tier.Two, true), rare(Rarity.UNCOMMON), "25_components");
    public static final DeferredBlock<FlatScreen> FlatScreenBackTier3 = register(Constants.BlockName$.MODULE$.FlatScreenBackTier3(), () -> new FlatScreen(defaultProps().noOcclusion(), Tier.Three, true), rare(Rarity.RARE), "25_components");
    public static final DeferredBlock<FlatScreen> FlatScreenBackTier4 = register(Constants.BlockName$.MODULE$.FlatScreenBackTier4(), () -> new FlatScreen(defaultProps().noOcclusion(), Tier.Four, true), legendary(), "25_components");
    public static final DeferredBlock<FlatScreen> FlatScreenFrontTier1 = register(Constants.BlockName$.MODULE$.FlatScreenFrontTier1(), () -> new FlatScreen(defaultProps().noOcclusion(), Tier.One, false), defaultItemProps(), "25_components");
    public static final DeferredBlock<FlatScreen> FlatScreenFrontTier2 = register(Constants.BlockName$.MODULE$.FlatScreenFrontTier2(), () -> new FlatScreen(defaultProps().noOcclusion(), Tier.Two, false), rare(Rarity.UNCOMMON), "25_components");
    public static final DeferredBlock<FlatScreen> FlatScreenFrontTier3 = register(Constants.BlockName$.MODULE$.FlatScreenFrontTier3(), () -> new FlatScreen(defaultProps().noOcclusion(), Tier.Three, false), rare(Rarity.RARE), "25_components");
    public static final DeferredBlock<FlatScreen> FlatScreenFrontTier4 = register(Constants.BlockName$.MODULE$.FlatScreenFrontTier4(), () -> new FlatScreen(defaultProps().noOcclusion(), Tier.Four, false), legendary(), "25_components");
    public static final DeferredBlock<Rack> Rack = register(Constants.BlockName$.MODULE$.Rack(), () -> new Rack(defaultProps()), defaultItemProps(), "10_networking");
    public static final DeferredBlock<Waypoint> Waypoint = register(Constants.BlockName$.MODULE$.Waypoint(), () -> new Waypoint(defaultProps()), defaultItemProps(), "25_components");
    public static final DeferredBlock<TapeDrive> TapeDrive = register(Constants.BlockName$.MODULE$.TapeDrive(), () -> new TapeDrive(defaultProps()), defaultItemProps(), "25_components");
    public static final DeferredBlock<AudioCable> AudioCable = register(Constants.BlockName$.MODULE$.AudioCable(), () -> new AudioCable(defaultProps()), defaultItemProps(), "10_networking");
    public static final DeferredBlock<Speaker> Speaker = register(Constants.BlockName$.MODULE$.Speaker(), () -> new Speaker(defaultProps()), defaultItemProps(), "25_components");
    public static final DeferredBlock<Microcontroller> Microcontroller = register(Constants.BlockName$.MODULE$.Microcontroller(), () -> new Microcontroller(defaultProps()), defaultItemProps(), "25_components");
    public static final DeferredBlock<Print> Print = register(Constants.BlockName$.MODULE$.Print(), () -> new Print(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1, 5).noOcclusion().dynamicShape()), defaultItemProps(), "25_components");
    public static final DeferredBlock<RobotAfterimage> RobotAfterimage = registerOnly(Constants.BlockName$.MODULE$.RobotAfterimage(), () -> new RobotAfterimage(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).noCollission().instabreak().noOcclusion().dynamicShape()));
    public static final DeferredBlock<RobotProxy> Robot = register(Constants.BlockName$.MODULE$.Robot(), () -> new RobotProxy(defaultProps().noOcclusion().dynamicShape()), defaultItemProps(), "25_components");
    public static final DeferredBlock<FakeEndstone> Endstone = register(Constants.BlockName$.MODULE$.Endstone(), () -> new FakeEndstone(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3, 15)), defaultItemProps(), "50_materials");
    public static final DeferredBlock<NetSplitter> NetSplitter = register(Constants.BlockName$.MODULE$.NetSplitter(), () -> new NetSplitter(defaultProps()), defaultItemProps(), "10_networking");
    public static final DeferredBlock<Transposer> Transposer = register(Constants.BlockName$.MODULE$.Transposer(), () -> new Transposer(defaultProps()), defaultItemProps(), "25_components");
    public static final DeferredBlock<CarpetedCapacitor> CarpetedCapacitor = register(Constants.BlockName$.MODULE$.CarpetedCapacitor(), () -> new CarpetedCapacitor(defaultProps()), defaultItemProps(), "10_networking");

    // Java-compatible accessors preserve existing calls emitted for the Scala object.
    public static DeferredBlock<Adapter> Adapter() { return Adapter; }
    public static DeferredBlock<Assembler> Assembler() { return Assembler; }
    public static DeferredBlock<Cable> Cable() { return Cable; }
    public static DeferredBlock<Capacitor> Capacitor() { return Capacitor; }
    public static DeferredBlock<Case> CaseTier1() { return CaseTier1; }
    public static DeferredBlock<Case> CaseTier2() { return CaseTier2; }
    public static DeferredBlock<Case> CaseTier3() { return CaseTier3; }
    public static DeferredBlock<Case> CaseTier4() { return CaseTier4; }
    public static DeferredBlock<Case> CaseCreative() { return CaseCreative; }
    public static DeferredBlock<ChameliumBlock> ChameliumBlock() { return ChameliumBlock; }
    public static DeferredBlock<Charger> Charger() { return Charger; }
    public static DeferredBlock<Disassembler> Disassembler() { return Disassembler; }
    public static DeferredBlock<DiskDrive> DiskDrive() { return DiskDrive; }
    public static DeferredBlock<Geolyzer> Geolyzer() { return Geolyzer; }
    public static DeferredBlock<Hologram> HologramTier1() { return HologramTier1; }
    public static DeferredBlock<Hologram> HologramTier2() { return HologramTier2; }
    public static DeferredBlock<Hologram> HologramTier3() { return HologramTier3; }
    public static DeferredBlock<Projector> Projector() { return Projector; }
    public static DeferredBlock<HoloScreen> HoloScreenTier1() { return HoloScreenTier1; }
    public static DeferredBlock<HoloScreen> HoloScreenTier2() { return HoloScreenTier2; }
    public static DeferredBlock<HoloScreen> HoloScreenTier3() { return HoloScreenTier3; }
    public static DeferredBlock<HoloScreen> HoloScreenTier4() { return HoloScreenTier4; }
    public static DeferredBlock<Keyboard> Keyboard() { return Keyboard; }
    public static DeferredBlock<MotionSensor> MotionSensor() { return MotionSensor; }
    public static DeferredBlock<PowerConverter> PowerConverter() { return PowerConverter; }
    public static DeferredBlock<PowerDistributor> PowerDistributor() { return PowerDistributor; }
    public static DeferredBlock<Printer> Printer() { return Printer; }
    public static DeferredBlock<Raid> Raid() { return Raid; }
    public static DeferredBlock<Redstone> Redstone() { return Redstone; }
    public static DeferredBlock<Relay> Relay() { return Relay; }
    public static DeferredBlock<Screen> ScreenTier1() { return ScreenTier1; }
    public static DeferredBlock<Screen> ScreenTier2() { return ScreenTier2; }
    public static DeferredBlock<Screen> ScreenTier3() { return ScreenTier3; }
    public static DeferredBlock<Screen> ScreenTier4() { return ScreenTier4; }
    public static DeferredBlock<FlatScreen> FlatScreenBackTier1() { return FlatScreenBackTier1; }
    public static DeferredBlock<FlatScreen> FlatScreenBackTier2() { return FlatScreenBackTier2; }
    public static DeferredBlock<FlatScreen> FlatScreenBackTier3() { return FlatScreenBackTier3; }
    public static DeferredBlock<FlatScreen> FlatScreenBackTier4() { return FlatScreenBackTier4; }
    public static DeferredBlock<FlatScreen> FlatScreenFrontTier1() { return FlatScreenFrontTier1; }
    public static DeferredBlock<FlatScreen> FlatScreenFrontTier2() { return FlatScreenFrontTier2; }
    public static DeferredBlock<FlatScreen> FlatScreenFrontTier3() { return FlatScreenFrontTier3; }
    public static DeferredBlock<FlatScreen> FlatScreenFrontTier4() { return FlatScreenFrontTier4; }
    public static DeferredBlock<Rack> Rack() { return Rack; }
    public static DeferredBlock<Waypoint> Waypoint() { return Waypoint; }
    public static DeferredBlock<TapeDrive> TapeDrive() { return TapeDrive; }
    public static DeferredBlock<AudioCable> AudioCable() { return AudioCable; }
    public static DeferredBlock<Speaker> Speaker() { return Speaker; }
    public static DeferredBlock<Microcontroller> Microcontroller() { return Microcontroller; }
    public static DeferredBlock<Print> Print() { return Print; }
    public static DeferredBlock<RobotAfterimage> RobotAfterimage() { return RobotAfterimage; }
    public static DeferredBlock<RobotProxy> Robot() { return Robot; }
    public static DeferredBlock<FakeEndstone> Endstone() { return Endstone; }
    public static DeferredBlock<NetSplitter> NetSplitter() { return NetSplitter; }
    public static DeferredBlock<Transposer> Transposer() { return Transposer; }
    public static DeferredBlock<CarpetedCapacitor> CarpetedCapacitor() { return CarpetedCapacitor; }

    public static void init(IEventBus bus) { BLOCKS.register(bus); }
}
