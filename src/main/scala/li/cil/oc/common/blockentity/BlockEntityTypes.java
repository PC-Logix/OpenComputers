package li.cil.oc.common.blockentity;

import li.cil.oc.OpenComputers;
import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, OpenComputers.ID());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Adapter>> ADAPTER =
            BLOCK_ENTITY_TYPES.register("adapter", () -> BlockEntityType.Builder
                    .of(Adapter::new, Items.get(Constants.BlockName$.MODULE$.Adapter()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Assembler>> ASSEMBLER =
            BLOCK_ENTITY_TYPES.register("assembler", () -> BlockEntityType.Builder
                    .of(Assembler::new, Items.get(Constants.BlockName$.MODULE$.Assembler()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Cable>> CABLE =
            BLOCK_ENTITY_TYPES.register("cable", () -> BlockEntityType.Builder
                    .of(Cable::new, Items.get(Constants.BlockName$.MODULE$.Cable()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Capacitor>> CAPACITOR =
            BLOCK_ENTITY_TYPES.register("capacitor", () -> BlockEntityType.Builder
                    .of(Capacitor::new, Items.get(Constants.BlockName$.MODULE$.Capacitor()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CarpetedCapacitor>> CARPETED_CAPACITOR =
            BLOCK_ENTITY_TYPES.register("carpeted_capacitor", () -> BlockEntityType.Builder
                    .of(CarpetedCapacitor::new, Items.get(Constants.BlockName$.MODULE$.CarpetedCapacitor()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Case>> CASE =
            BLOCK_ENTITY_TYPES.register("case", () -> BlockEntityType.Builder
                    .of(Case::new,
                            Items.get(Constants.BlockName$.MODULE$.CaseCreative()).block(),
                            Items.get(Constants.BlockName$.MODULE$.CaseTier1()).block(),
                            Items.get(Constants.BlockName$.MODULE$.CaseTier2()).block(),
                            Items.get(Constants.BlockName$.MODULE$.CaseTier3()).block(),
                            Items.get(Constants.BlockName$.MODULE$.CaseTier4()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Charger>> CHARGER =
            BLOCK_ENTITY_TYPES.register("charger", () -> BlockEntityType.Builder
                    .of(Charger::new, Items.get(Constants.BlockName$.MODULE$.Charger()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Disassembler>> DISASSEMBLER =
            BLOCK_ENTITY_TYPES.register("disassembler", () -> BlockEntityType.Builder
                    .of(Disassembler::new, Items.get(Constants.BlockName$.MODULE$.Disassembler()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DiskDrive>> DISK_DRIVE =
            BLOCK_ENTITY_TYPES.register("disk_drive", () -> BlockEntityType.Builder
                    .of(DiskDrive::new, Items.get(Constants.BlockName$.MODULE$.DiskDrive()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Geolyzer>> GEOLYZER =
            BLOCK_ENTITY_TYPES.register("geolyzer", () -> BlockEntityType.Builder
                    .of(Geolyzer::new, Items.get(Constants.BlockName$.MODULE$.Geolyzer()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Hologram>> HOLOGRAM =
            BLOCK_ENTITY_TYPES.register("hologram", () -> BlockEntityType.Builder
                    .of(Hologram::new,
                            Items.get(Constants.BlockName$.MODULE$.HologramTier1()).block(),
                            Items.get(Constants.BlockName$.MODULE$.HologramTier2()).block(),
                            Items.get(Constants.BlockName$.MODULE$.HologramTier3()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Keyboard>> KEYBOARD =
            BLOCK_ENTITY_TYPES.register("keyboard", () -> BlockEntityType.Builder
                    .of(Keyboard::new,
                            Items.get(Constants.BlockName$.MODULE$.Keyboard()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Microcontroller>> MICROCONTROLLER =
            BLOCK_ENTITY_TYPES.register("microcontroller", () -> BlockEntityType.Builder
                    .of(Microcontroller::new,
                            Items.get(Constants.BlockName$.MODULE$.Microcontroller()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MotionSensor>> MOTION_SENSOR =
            BLOCK_ENTITY_TYPES.register("motion_sensor", () -> BlockEntityType.Builder
                    .of(MotionSensor::new,
                            Items.get(Constants.BlockName$.MODULE$.MotionSensor()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetSplitter>> NET_SPLITTER =
            BLOCK_ENTITY_TYPES.register("net_splitter", () -> BlockEntityType.Builder
                    .of(NetSplitter::new,
                            Items.get(Constants.BlockName$.MODULE$.NetSplitter()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerConverter>> POWER_CONVERTER =
            BLOCK_ENTITY_TYPES.register("power_converter", () -> BlockEntityType.Builder
                    .of(PowerConverter::new,
                            Items.get(Constants.BlockName$.MODULE$.PowerConverter()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerDistributor>> POWER_DISTRIBUTOR =
            BLOCK_ENTITY_TYPES.register("power_distributor", () -> BlockEntityType.Builder
                    .of(PowerDistributor::new,
                            Items.get(Constants.BlockName$.MODULE$.PowerDistributor()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Print>> PRINT =
            BLOCK_ENTITY_TYPES.register("print", () -> BlockEntityType.Builder
                    .of(Print::new,
                            Items.get(Constants.BlockName$.MODULE$.Print()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Printer>> PRINTER =
            BLOCK_ENTITY_TYPES.register("printer", () -> BlockEntityType.Builder
                    .of(Printer::new,
                            Items.get(Constants.BlockName$.MODULE$.Printer()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Rack>> RACK =
            BLOCK_ENTITY_TYPES.register("rack", () -> BlockEntityType.Builder
                    .of(Rack::new,
                            Items.get(Constants.BlockName$.MODULE$.Rack()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Raid>> RAID =
            BLOCK_ENTITY_TYPES.register("raid", () -> BlockEntityType.Builder
                    .of(Raid::new,
                            Items.get(Constants.BlockName$.MODULE$.Raid()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Redstone>> REDSTONE_IO =
            BLOCK_ENTITY_TYPES.register("redstone_io", () -> BlockEntityType.Builder
                    .of(Redstone::new,
                            Items.get(Constants.BlockName$.MODULE$.Redstone()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Relay>> RELAY =
            BLOCK_ENTITY_TYPES.register("relay", () -> BlockEntityType.Builder
                    .of(Relay::new,
                            Items.get(Constants.BlockName$.MODULE$.Relay()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RobotProxy>> ROBOT =
            BLOCK_ENTITY_TYPES.register("robot", () -> BlockEntityType.Builder
                    .of(RobotProxy::new,
                            Items.get(Constants.BlockName$.MODULE$.Robot()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Screen>> SCREEN =
            BLOCK_ENTITY_TYPES.register("screen", () -> BlockEntityType.Builder
                    .of(BlockEntityTypes::createScreen,
                            Items.get(Constants.BlockName$.MODULE$.ScreenTier1()).block(),
                            Items.get(Constants.BlockName$.MODULE$.ScreenTier2()).block(),
                            Items.get(Constants.BlockName$.MODULE$.ScreenTier3()).block(),
                            Items.get(Constants.BlockName$.MODULE$.ScreenTier4()).block(),
                            Items.get(Constants.BlockName$.MODULE$.FlatScreenBackTier1()).block(),
                            Items.get(Constants.BlockName$.MODULE$.FlatScreenBackTier2()).block(),
                            Items.get(Constants.BlockName$.MODULE$.FlatScreenBackTier3()).block(),
                            Items.get(Constants.BlockName$.MODULE$.FlatScreenBackTier4()).block(),
                            Items.get(Constants.BlockName$.MODULE$.FlatScreenFrontTier1()).block(),
                            Items.get(Constants.BlockName$.MODULE$.FlatScreenFrontTier2()).block(),
                            Items.get(Constants.BlockName$.MODULE$.FlatScreenFrontTier3()).block(),
                            Items.get(Constants.BlockName$.MODULE$.FlatScreenFrontTier4()).block(),
                            Items.get(Constants.BlockName$.MODULE$.HoloScreenTier1()).block(),
                            Items.get(Constants.BlockName$.MODULE$.HoloScreenTier2()).block(),
                            Items.get(Constants.BlockName$.MODULE$.HoloScreenTier3()).block(),
                            Items.get(Constants.BlockName$.MODULE$.HoloScreenTier4()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Transposer>> TRANSPOSER =
            BLOCK_ENTITY_TYPES.register("transposer", () -> BlockEntityType.Builder
                    .of(Transposer::new,
                            Items.get(Constants.BlockName$.MODULE$.Transposer()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Waypoint>> WAYPOINT =
            BLOCK_ENTITY_TYPES.register("waypoint", () -> BlockEntityType.Builder
                    .of(Waypoint::new,
                            Items.get(Constants.BlockName$.MODULE$.Waypoint()).block())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComputronicsBlockEntity>> COMPUTRONICS =
            BLOCK_ENTITY_TYPES.register("computronics", () -> BlockEntityType.Builder
                    .of((pos, state) -> ComputronicsBlockEntity$.MODULE$.create(pos, state),
                            Items.get(Constants.BlockName$.MODULE$.ComputronicsIronNote()).block(),
                            Items.get(Constants.BlockName$.MODULE$.ComputronicsAudioCable()).block(),
                            Items.get(Constants.BlockName$.MODULE$.ComputronicsSpeaker()).block(),
                            Items.get(Constants.BlockName$.MODULE$.ComputronicsTapeReader()).block(),
                            Items.get(Constants.BlockName$.MODULE$.ComputronicsCamera()).block(),
                            Items.get(Constants.BlockName$.MODULE$.ComputronicsChatBox()).block(),
                            Items.get(Constants.BlockName$.MODULE$.ComputronicsCipher()).block(),
                            Items.get(Constants.BlockName$.MODULE$.ComputronicsCipherAdvanced()).block(),
                            Items.get(Constants.BlockName$.MODULE$.ComputronicsRadar()).block(),
                            Items.get(Constants.BlockName$.MODULE$.ComputronicsColorfulLamp()).block(),
                            Items.get(Constants.BlockName$.MODULE$.ComputronicsSpeechBox()).block())
                    .build(null));

    public static void init(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }

    private static Screen createScreen(BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof li.cil.oc.common.block.HoloScreen holoScreen) {
            return new HoloScreen(pos, state, holoScreen.tier());
        }
        if (block instanceof li.cil.oc.common.block.Screen screen) {
            return new Screen(pos, state, screen.tier());
        }
        return new Screen(pos, state);
    }

    private BlockEntityTypes() {
        throw new Error();
    }
}
