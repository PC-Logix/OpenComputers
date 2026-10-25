package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import li.cil.oc.api.ImmutableFluidStack;
import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.item.data.PrintDataSupport;
import li.cil.oc.server.component.DebugCard;
import li.cil.oc.server.component.DebugCardAccessContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import scala.Option;
import scala.Tuple2;
import scala.collection.immutable.List;
import scala.collection.immutable.Map;
import scala.collection.immutable.Set;
import scala.runtime.BoxedUnit;

import java.nio.ByteBuffer;
import java.util.UUID;

/** Persistent and synchronized Minecraft data components used by OpenComputers. */
public class OCComponents {
    public static final DeferredRegister.DataComponents REGISTRAR =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "opencomputers");

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> persistent(String name, Codec<T> codec) {
        return register(name, builder -> builder.persistent(codec));
    }

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> sharedOnly(
            String name, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return register(name, builder -> builder.networkSynchronized(streamCodec));
    }

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> persistentShared(
            String name, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return register(name, builder -> builder.persistent(codec).networkSynchronized(streamCodec));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name, java.util.function.UnaryOperator<DataComponentType.Builder<T>> configure) {
        return (DeferredHolder) REGISTRAR.registerComponentType(name, configure);
    }

    private static DeferredHolder<DataComponentType<?>, DataComponentType<BoxedUnit>> persistentUnit(String name) {
        return persistent(name, Codec.unit(BoxedUnit.UNIT));
    }

    private static DeferredHolder<DataComponentType<?>, DataComponentType<BoxedUnit>> persistentSharedUnit(String name) {
        return persistentShared(name, Codec.unit(BoxedUnit.UNIT), StreamCodec.unit(BoxedUnit.UNIT));
    }

    public static void init(IEventBus modBus) {
        Network.initialize();
        REGISTRAR.register(modBus);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> ID = persistentShared("id", UUIDUtil.CODEC, UUIDUtil.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Owner>> OWNER = persistentShared("owner", Owner.CODEC, Owner.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DyeColor>> DISK_COLOR = persistentShared("disk_color", DyeColor.CODEC, DyeColor.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> LOOT_DISK = persistentShared("loot_disk", ResourceLocation.CODEC, ResourceLocation.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> LABEL = persistentShared("label", Codec.STRING, ByteBufCodecs.STRING_UTF8);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> READONLY = persistentShared("readonly", ScalaCodec.BOOL, ScalaStreamCodec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ByteBuffer>> EEPROM_CODE = persistent("eeprom_code", Codec.BYTE_BUFFER);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ByteBuffer>> EEPROM_DATA = persistent("eeprom_data", Codec.BYTE_BUFFER);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MFCoords>> MF_COORD = persistentShared("mf_coord", MFCoords.CODEC, MFCoords.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ImmutableItemStack>>> COMPONENTS = persistentShared("components", ScalaCodec.list(ImmutableItemStack.OPTIONAL_CODEC), ScalaStreamCodec.list(ImmutableItemStack.OPTIONAL_STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ImmutableItemStack>>> CONTENTS = persistentShared("contents", ScalaCodec.list(ImmutableItemStack.OPTIONAL_CODEC), ScalaStreamCodec.list(ImmutableItemStack.OPTIONAL_STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ImmutableItemStack>>> CONTAINERS = persistentShared("containers", ScalaCodec.list(ImmutableItemStack.OPTIONAL_CODEC), ScalaStreamCodec.list(ImmutableItemStack.OPTIONAL_STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ImmutableItemStack>> ATTACHMENT = persistentShared("attachment", ImmutableItemStack.OPTIONAL_CODEC, ImmutableItemStack.OPTIONAL_STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Byte>> TIER = persistentShared("tier", ScalaCodec.BYTE, ScalaStreamCodec.BYTE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORED_ENERGY = persistentShared("stored_energy", ScalaCodec.INT, ScalaStreamCodec.VAR_INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ADDRESS = persistentShared("address", Codec.STRING, ByteBufCodecs.STRING_UTF8);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Visibility>> VISIBILITY = persistentShared("visibility", Visibility.CODEC, Visibility.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> UNMANAGED = persistentShared("unmanaged", ScalaCodec.BOOL, ScalaStreamCodec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> LOCK = persistentShared("lock", Codec.STRING, ByteBufCodecs.STRING_UTF8);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> CHARGE = persistentShared("charge", ScalaCodec.DOUBLE, ScalaStreamCodec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> MAX_CHARGE = persistentShared("max_charge", ScalaCodec.DOUBLE, ScalaStreamCodec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RobotChargeInfo>> ROBOT_CHARGE = persistentShared("robot_charge", RobotChargeInfo.CODEC, RobotChargeInfo.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> NANOMACHINES_NETWORK_INFO = persistent("nanomachines_network_info", CompoundTag.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ImmutableItemStack>> SOURCE_MAP_ITEM = persistent("source_map_item", ImmutableItemStack.OPTIONAL_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> KEYS = persistentShared("keys", ScalaCodec.list(Codec.STRING), ScalaStreamCodec.list(ByteBufCodecs.STRING_UTF8));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TerminalReference>> TERMINAL_REFERENCE = persistent("terminal_reference", TerminalReference.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DataComponentPatch>> TERMINAL_SERVER_BUFFER = persistentShared("terminal_server_buffer", DataComponentPatch.CODEC, DataComponentPatch.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DataComponentPatch>> TERMINAL_SERVER_KEYBOARD = persistentShared("terminal_server_keyboard", DataComponentPatch.CODEC, DataComponentPatch.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DataComponentPatch>> RACK_KVM_BUFFER_0 = persistentShared("rack_kvm_buffer_0", DataComponentPatch.CODEC, DataComponentPatch.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DataComponentPatch>> RACK_KVM_BUFFER_1 = persistentShared("rack_kvm_buffer_1", DataComponentPatch.CODEC, DataComponentPatch.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DataComponentPatch>> RACK_KVM_BUFFER_2 = persistentShared("rack_kvm_buffer_2", DataComponentPatch.CODEC, DataComponentPatch.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DataComponentPatch>> RACK_KVM_KEYBOARD_0 = persistentShared("rack_kvm_keyboard_0", DataComponentPatch.CODEC, DataComponentPatch.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DataComponentPatch>> RACK_KVM_KEYBOARD_1 = persistentShared("rack_kvm_keyboard_1", DataComponentPatch.CODEC, DataComponentPatch.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DataComponentPatch>> RACK_KVM_KEYBOARD_2 = persistentShared("rack_kvm_keyboard_2", DataComponentPatch.CODEC, DataComponentPatch.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RACK_KVM_SELECTED_SLOT = persistentShared("rack_kvm_selected_slot", ScalaCodec.INT, ScalaStreamCodec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RACK_KVM_SERVER_MASK = persistentShared("rack_kvm_server_mask", ScalaCodec.INT, ScalaStreamCodec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TextBufferContents>> TEXT_BUFFER = persistent("text_buffer", TextBufferContents.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_ON = persistentShared("is_on", ScalaCodec.BOOL, ScalaStreamCodec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_RUNNING = persistentShared("is_running", ScalaCodec.BOOL, ScalaStreamCodec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_POWERED = persistentShared("is_powered", ScalaCodec.BOOL, ScalaStreamCodec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BoxedUnit>> IS_ERRORED = persistentSharedUnit("is_errored");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Set<String>>> USERS = persistentShared("users", ScalaCodec.set(Codec.STRING), ScalaStreamCodec.set(ByteBufCodecs.STRING_UTF8));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MaximumVideoMode>> MAX_VIDEO_MODE = persistentShared("max_video_mode", MaximumVideoMode.CODEC, MaximumVideoMode.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<VideoMode>> VIDEO_MODE = persistentShared("video_mode", VideoMode.CODEC, VideoMode.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_PRECISE = persistentShared("is_precise", ScalaCodec.BOOL, ScalaStreamCodec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MachineData>> MACHINE = persistent("machine", MachineData.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DroneState>> DRONE_STATE = persistentShared("drone_state", DroneState.CODEC, DroneState.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Component>> STATUS_TEXT = persistentShared("status_text", ComponentSerialization.FLAT_CODEC, ComponentSerialization.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ColorRGBA>> LIGHT_COLOR = persistentShared("light_color", ColorRGBA.CODEC, ScalaStreamCodec.COLOR_RGBA);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> ROBOT_FLAG = persistentShared("robot_flag", ResourceLocation.CODEC, ResourceLocation.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PrintData>> PRINT = persistentShared("print", PrintDataSupport.CODEC, PrintDataSupport.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> FILESYSTEM_DATA = persistent("filesystem", CompoundTag.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> ROBOT_ROM_FILESYSTEM_DATA = persistent("robot_rom_filesystem", CompoundTag.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Map<String, Set<Integer>>>> HANDLES = persistent("handles", ScalaCodec.map(Codec.STRING, ScalaCodec.set(ScalaCodec.INT)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Tuple2<Long, Map<String, CompoundStorage>>>> COMPOUND_DRIVER = persistent("compound_driver", ScalaCodec.pair(new Tuple2<>(ScalaCodec.LONG, ScalaCodec.map(Codec.STRING, CompoundStorage.CODEC))));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Integer>>> PALETTE = persistentShared("palette", ScalaCodec.list(ScalaCodec.INT), ScalaStreamCodec.list(ScalaStreamCodec.INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GraphicsCardState>> GRAPHICS_CARD = persistent("graphics_card", GraphicsCardState.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Tuple2<Integer, CompoundStorage>>>> VIDEO_RAM = persistent("video_ram", ScalaCodec.list(ScalaCodec.pair(new Tuple2<>(ScalaCodec.INT, CompoundStorage.CODEC))));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WAKE_THRESHOLD = persistent("wake_threshold", ScalaCodec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HAS_REDSTONE_INPUT = persistent("has_redstone_input", ScalaCodec.BOOL);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BoxedUnit>> INVERT_TOUCH = persistentSharedUnit("invert_touch");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WirelessRedstoneState>> WIRELESS_REDSTONE_STATE = persistent("wireless_redstone_state", WirelessRedstoneState.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<UUID>>> LEASHED_ENTITIES = persistent("leashed_entities", ScalaCodec.list(UUIDUtil.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Integer>>> OPEN_PORTS = persistent("open_ports", ScalaCodec.list(ScalaCodec.INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WakeMessage>> WAKE_MESSAGE = persistent("wake_message", WakeMessage.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TUNNEL = persistent("tunnel", Codec.STRING);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> STRENGTH = persistent("strength", ScalaCodec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HEAD_POS = persistent("head_position", ScalaCodec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ImmutableFluidStack>> TANK = persistent("tank", ImmutableFluidStack.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ImmutableItemStack>> FUEL_INVENTORY = persistent("fuel_inventory", ImmutableItemStack.OPTIONAL_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FUEL_TICKS_REMAINING = persistent("fuel_ticks_remaining", ScalaCodec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ColorRGBA>> RENDER_COLOR = persistentShared("render_color", ColorRGBA.CODEC, ScalaStreamCodec.COLOR_RGBA);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> CHARGE_SPEED = persistentShared("charge_speed", ScalaCodec.DOUBLE, ScalaStreamCodec.DOUBLE);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BoxedUnit>> INVERT_SIGNAL = persistentUnit("invert_signal");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ByteBuffer>> PRESENCE = persistent("presence", Codec.BYTE_BUFFER);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Option<CompoundStorage>>>> COMPONENT_NODES = persistent("component_nodes", ScalaCodec.list(CompoundStorage.OPTION_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SELECTED_SLOT = persistentShared("selected_slot", ScalaCodec.INT, ScalaStreamCodec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SELECTED_TANK = persistent("selected_tank", ScalaCodec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ROBOT_TOTAL_ANIMATION_TIME = persistentShared("robot/total_animation_time", ScalaCodec.INT, ScalaStreamCodec.INT);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RobotCurrentAnimation>> ROBOT_CURRENT_ANIMATION = persistentShared("robot/current_animation", RobotCurrentAnimation.CODEC, RobotCurrentAnimation.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Option<CompoundStorage>>>> RACK_DATA = persistentShared("rack_data", ScalaCodec.list(CompoundStorage.OPTION_CODEC), ScalaStreamCodec.list(CompoundStorage.OPTION_STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BoxedUnit>> RELAY_ENABLED = persistentUnit("relay_enabled");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<List<Direction>>>> RACK_NODE_MAPPING = persistent("rack_node_mapping", ScalaCodec.list(ScalaCodec.list(Direction.CODEC)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DebugCardAccessContext>> DEBUG_CARD_ACCESS_CONTEXT = persistent("debug_card/access_context", DebugCardAccessContext.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> DEBUG_CARD_REMOTE_NODE_POSITION = persistent("debug_card/remote_node", BlockPos.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DyeColor>> CHAMELIUM_COLOR = persistentShared("chamelium_color", DyeColor.CODEC, DyeColor.STREAM_CODEC);

    public static final class Network {
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> LAST_ACCESS = sharedOnly("network/last_access_timestamp", ScalaStreamCodec.VAR_LONG);
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> LAST_DISK_ACCESS = sharedOnly("network/last_disk_access", ScalaStreamCodec.VAR_LONG);
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> LAST_NETWORK_ACCESS = sharedOnly("network/last_network_access", ScalaStreamCodec.VAR_LONG);
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<ImmutableItemStack>> DISK_ITEM = sharedOnly("network/disk_item", ImmutableItemStack.OPTIONAL_STREAM_CODEC);

        private static void initialize() {}

        private Network() {}
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> NETWORK_LAST_ACCESS = Network.LAST_ACCESS;
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> NETWORK_LAST_DISK_ACCESS = Network.LAST_DISK_ACCESS;
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> NETWORK_LAST_NETWORK_ACCESS = Network.LAST_NETWORK_ACCESS;
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ImmutableItemStack>> NETWORK_DISK_ITEM = Network.DISK_ITEM;

    protected OCComponents() {}
}

