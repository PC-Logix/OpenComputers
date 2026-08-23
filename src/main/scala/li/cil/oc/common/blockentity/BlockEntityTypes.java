package li.cil.oc.common.blockentity;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.init.OCBlocks;
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
                    .of(Adapter::new, OCBlocks.Adapter().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Assembler>> ASSEMBLER =
            BLOCK_ENTITY_TYPES.register("assembler", () -> BlockEntityType.Builder
                    .of(Assembler::new, OCBlocks.Assembler().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Cable>> CABLE =
            BLOCK_ENTITY_TYPES.register("cable", () -> BlockEntityType.Builder
                    .of(Cable::new, OCBlocks.Cable().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Capacitor>> CAPACITOR =
            BLOCK_ENTITY_TYPES.register("capacitor", () -> BlockEntityType.Builder
                    .of(Capacitor::new, OCBlocks.Capacitor().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CarpetedCapacitor>> CARPETED_CAPACITOR =
            BLOCK_ENTITY_TYPES.register("carpeted_capacitor", () -> BlockEntityType.Builder
                    .of(CarpetedCapacitor::new, OCBlocks.CarpetedCapacitor().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Case>> CASE =
            BLOCK_ENTITY_TYPES.register("case", () -> BlockEntityType.Builder
                    .of(Case::new,
                            OCBlocks.CaseCreative().get(),
                            OCBlocks.CaseTier1().get(),
                            OCBlocks.CaseTier2().get(),
                            OCBlocks.CaseTier3().get(),
                            OCBlocks.CaseTier4().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Charger>> CHARGER =
            BLOCK_ENTITY_TYPES.register("charger", () -> BlockEntityType.Builder
                    .of(Charger::new, OCBlocks.Charger().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Disassembler>> DISASSEMBLER =
            BLOCK_ENTITY_TYPES.register("disassembler", () -> BlockEntityType.Builder
                    .of(Disassembler::new, OCBlocks.Disassembler().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DiskDrive>> DISK_DRIVE =
            BLOCK_ENTITY_TYPES.register("disk_drive", () -> BlockEntityType.Builder
                    .of(DiskDrive::new, OCBlocks.DiskDrive().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Geolyzer>> GEOLYZER =
            BLOCK_ENTITY_TYPES.register("geolyzer", () -> BlockEntityType.Builder
                    .of(Geolyzer::new, OCBlocks.Geolyzer().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Hologram>> HOLOGRAM =
            BLOCK_ENTITY_TYPES.register("hologram", () -> BlockEntityType.Builder
                    .of(Hologram::new,
                            OCBlocks.HologramTier1().get(),
                            OCBlocks.HologramTier2().get(),
                            OCBlocks.HologramTier3().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Keyboard>> KEYBOARD =
            BLOCK_ENTITY_TYPES.register("keyboard", () -> BlockEntityType.Builder
                    .of(Keyboard::new, OCBlocks.Keyboard().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Microcontroller>> MICROCONTROLLER =
            BLOCK_ENTITY_TYPES.register("microcontroller", () -> BlockEntityType.Builder
                    .of(Microcontroller::new, OCBlocks.Microcontroller().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MotionSensor>> MOTION_SENSOR =
            BLOCK_ENTITY_TYPES.register("motion_sensor", () -> BlockEntityType.Builder
                    .of(MotionSensor::new, OCBlocks.MotionSensor().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetSplitter>> NET_SPLITTER =
            BLOCK_ENTITY_TYPES.register("net_splitter", () -> BlockEntityType.Builder
                    .of(NetSplitter::new, OCBlocks.NetSplitter().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerConverter>> POWER_CONVERTER =
            BLOCK_ENTITY_TYPES.register("power_converter", () -> BlockEntityType.Builder
                    .of(PowerConverter::new, OCBlocks.PowerConverter().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerDistributor>> POWER_DISTRIBUTOR =
            BLOCK_ENTITY_TYPES.register("power_distributor", () -> BlockEntityType.Builder
                    .of(PowerDistributor::new, OCBlocks.PowerDistributor().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Print>> PRINT =
            BLOCK_ENTITY_TYPES.register("print", () -> BlockEntityType.Builder
                    .of(Print::new, OCBlocks.Print().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Printer>> PRINTER =
            BLOCK_ENTITY_TYPES.register("printer", () -> BlockEntityType.Builder
                    .of(Printer::new, OCBlocks.Printer().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Rack>> RACK =
            BLOCK_ENTITY_TYPES.register("rack", () -> BlockEntityType.Builder
                    .of(Rack::new, OCBlocks.Rack().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Raid>> RAID =
            BLOCK_ENTITY_TYPES.register("raid", () -> BlockEntityType.Builder
                    .of(Raid::new, OCBlocks.Raid().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Redstone>> REDSTONE_IO =
            BLOCK_ENTITY_TYPES.register("redstone_io", () -> BlockEntityType.Builder
                    .of(Redstone::new, OCBlocks.Redstone().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Relay>> RELAY =
            BLOCK_ENTITY_TYPES.register("relay", () -> BlockEntityType.Builder
                    .of(Relay::new, OCBlocks.Relay().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RobotProxy>> ROBOT =
            BLOCK_ENTITY_TYPES.register("robot", () -> BlockEntityType.Builder
                    .of(RobotProxy::new, OCBlocks.Robot().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Screen>> SCREEN =
            BLOCK_ENTITY_TYPES.register("screen", () -> BlockEntityType.Builder
                    .of(BlockEntityTypes::createScreen,
                            OCBlocks.ScreenTier1().get(),
                            OCBlocks.ScreenTier2().get(),
                            OCBlocks.ScreenTier3().get(),
                            OCBlocks.ScreenTier4().get(),
                            OCBlocks.FlatScreenBackTier1().get(),
                            OCBlocks.FlatScreenBackTier2().get(),
                            OCBlocks.FlatScreenBackTier3().get(),
                            OCBlocks.FlatScreenBackTier4().get(),
                            OCBlocks.FlatScreenFrontTier1().get(),
                            OCBlocks.FlatScreenFrontTier2().get(),
                            OCBlocks.FlatScreenFrontTier3().get(),
                            OCBlocks.FlatScreenFrontTier4().get(),
                            OCBlocks.HoloScreenTier1().get(),
                            OCBlocks.HoloScreenTier2().get(),
                            OCBlocks.HoloScreenTier3().get(),
                            OCBlocks.HoloScreenTier4().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Transposer>> TRANSPOSER =
            BLOCK_ENTITY_TYPES.register("transposer", () -> BlockEntityType.Builder
                    .of(Transposer::new, OCBlocks.Transposer().get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Waypoint>> WAYPOINT =
            BLOCK_ENTITY_TYPES.register("waypoint", () -> BlockEntityType.Builder
                    .of(Waypoint::new, OCBlocks.Waypoint().get())
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
