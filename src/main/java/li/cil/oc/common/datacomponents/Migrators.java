package li.cil.oc.common.datacomponents;

import li.cil.oc.Settings;
import li.cil.oc.api.ImmutableFluidStack;
import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.item.data.PrintDataSupport;
import li.cil.oc.server.component.DebugCardAccessContext;
import li.cil.oc.server.machine.MachineState;
import li.cil.oc.util.Color;
import li.cil.oc.util.ItemUtils;
import li.cil.oc.util.NbtDataStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.fluids.FluidStack;
import scala.Option;
import scala.Option$;
import scala.Tuple2;
import scala.collection.immutable.List$;
import scala.collection.immutable.Map$;
import scala.collection.immutable.Set$;
import scala.jdk.javaapi.CollectionConverters;
import scala.runtime.BoxedUnit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Migrators {
    public static final scala.collection.mutable.Map<DataComponentType<?>, Migrator<?>> map =
        new scala.collection.mutable.HashMap<>();
    private static final String OC = Settings.namespace();
    private Migrators() { }

    private static <T> scala.collection.immutable.List<T> list(List<T> values) {
        return List$.MODULE$.from(CollectionConverters.asScala(values));
    }
    private static <T> scala.collection.immutable.Set<T> set(Set<T> values) {
        return Set$.MODULE$.from(CollectionConverters.asScala(values));
    }
    private static <K, V> scala.collection.immutable.Map<K, V> map(Map<K, V> values) {
        return Map$.MODULE$.from(CollectionConverters.asScala(values));
    }
    private static <T> Option<T> option(Optional<T> value) { return Option$.MODULE$.apply(value.orElse(null)); }
    private static String oc(String name) { return OC + (OC.endsWith(":") ? "" : ":") + name; }

    private static void register(Supplier<? extends DataComponentType<?>> component,
                                 Function<Decoder, ? extends Optional<?>> migrate) {
        map.put(component.get(), new Migrator<Object>() {
            @Override public Option<Object> fromNBT(CompoundTag tag, HolderLookup.Provider provider) {
                Optional<?> value = migrate.apply(new Decoder(tag, provider));
                if (value.isEmpty() && tag.contains(oc("data")))
                    value = migrate.apply(new Decoder(tag.getCompound(oc("data")), provider));
                return Option$.MODULE$.apply(value.orElse(null));
            }
        });
    }

    @SafeVarargs private static <T> Optional<T> first(Supplier<Optional<T>>... choices) {
        for (Supplier<Optional<T>> choice : choices) {
            Optional<T> value = choice.get();
            if (value.isPresent()) return value;
        }
        return Optional.empty();
    }

    private static final class Missing extends RuntimeException { }
    private static final class Field<T> {
        final CompoundTag tag;
        final String name;
        final Optional<T> value;
        Field(CompoundTag tag, String name, Optional<T> value) {
            this.tag = tag; this.name = name; this.value = value;
        }
        Optional<T> andRemove() { if (value.isPresent()) tag.remove(name); return value; }
        <U> Field<U> map(Function<T, Optional<U>> converter) {
            return new Field<>(tag, name, value.flatMap(converter));
        }
        Field<T> orElse(T fallback) {
            return new Field<>(tag, name, Optional.of(value.orElse(fallback)));
        }
    }
    private static final class Composition {
        final List<Field<?>> fields = new ArrayList<>();
        <T> T require(Field<T> field) {
            fields.add(field);
            return field.value.orElseThrow(Missing::new);
        }
        <T> Optional<T> optional(Field<T> field) { fields.add(field); return field.value; }
        CompoundTag remaining(Decoder decoder) {
            fields.forEach(Field::andRemove);
            fields.clear();
            return decoder.tag;
        }
        void remove() { fields.forEach(Field::andRemove); }
    }
    private static <T> Optional<T> compose(Function<Composition, T> body) {
        return compose(body, true);
    }
    private static <T> Optional<T> composeValue(Function<Composition, T> body) {
        return compose(body, false);
    }
    private static <T> Optional<T> compose(Function<Composition, T> body, boolean remove) {
        try {
            Composition composition = new Composition();
            T result = body.apply(composition);
            if (remove) composition.remove();
            return Optional.ofNullable(result);
        } catch (Missing error) { return Optional.empty(); }
    }

    private static final class Decoder {
        final CompoundTag tag;
        final HolderLookup.Provider provider;
        Decoder(CompoundTag tag, HolderLookup.Provider provider) { this.tag = tag; this.provider = provider; }
        <T> Field<T> field(String name, Function<Tag, Optional<T>> converter) {
            Tag value = tag.get(name);
            return new Field<>(tag, name, value == null ? Optional.empty() : converter.apply(value));
        }
        <T> Optional<T> compound(String name, Function<Decoder, Optional<T>> converter) {
            Tag value = tag.get(name);
            return value instanceof CompoundTag compound
                ? converter.apply(new Decoder(compound, provider)) : Optional.empty();
        }
        Field<Tag> tag(String name) { return field(name, Optional::of); }
        Field<CompoundTag> compoundTag(String name) {
            return field(name, value -> value instanceof CompoundTag compound
                ? Optional.of(compound) : Optional.empty());
        }
        Field<String> string(String name) {
            return field(name, value -> value instanceof StringTag text
                ? Optional.of(text.getAsString()) : Optional.empty());
        }
        Field<List<String>> stringList(String name) {
            return field(name, value -> {
                if (!(value instanceof ListTag values) || values.getElementType() != Tag.TAG_STRING)
                    return Optional.empty();
                List<String> result = new ArrayList<>();
                for (int i = 0; i < values.size(); i++) result.add(values.getString(i));
                return Optional.of(result);
            });
        }
        Field<ResourceLocation> resourceLocation(String name) {
            return string(name).map(value -> Optional.ofNullable(ResourceLocation.tryParse(value)));
        }
        Field<Byte> byteValue(String name) {
            return field(name, value -> value instanceof ByteTag number
                ? Optional.of(number.getAsByte()) : Optional.empty());
        }
        Field<Boolean> bool(String name) { return byteValue(name).map(value -> Optional.of(value != 0)); }
        Field<Integer> intValue(String name) {
            return field(name, value -> value instanceof IntTag number
                ? Optional.of(number.getAsInt()) : Optional.empty());
        }
        Field<Long> longValue(String name) {
            return field(name, value -> value instanceof LongTag number
                ? Optional.of(number.getAsLong()) : Optional.empty());
        }
        Field<Float> floatValue(String name) {
            return field(name, value -> value instanceof FloatTag number
                ? Optional.of(number.getAsFloat()) : Optional.empty());
        }
        Field<Double> doubleValue(String name) {
            return field(name, value -> value instanceof DoubleTag number
                ? Optional.of(number.getAsDouble()) : Optional.empty());
        }
        Field<int[]> intArray(String name) {
            return field(name, value -> value instanceof IntArrayTag numbers
                ? Optional.of(numbers.getAsIntArray()) : Optional.empty());
        }
        Field<ByteBuffer> byteBuffer(String name) {
            return field(name, value -> {
                if (value instanceof StringTag text) return Optional.of(ByteBuffer.wrap(text.getAsString().getBytes()));
                if (value instanceof ByteArrayTag bytes) return Optional.of(ByteBuffer.wrap(bytes.getAsByteArray()));
                return Optional.empty();
            });
        }
        Field<UUID> uuid(String name) {
            return intArray(name).map(values -> values.length == 4
                ? Optional.of(UUIDUtil.uuidFromIntArray(values)) : Optional.empty());
        }
        Field<ImmutableItemStack> itemStack(String name) {
            return field(name, value -> value instanceof CompoundTag compound
                ? ImmutableItemStack.parse(provider, compound) : Optional.empty());
        }
        Field<List<ImmutableItemStack>> itemStackList(String name) {
            return field(name, value -> {
                if (!(value instanceof ListTag tags) || tags.getElementType() != Tag.TAG_COMPOUND)
                    return Optional.empty();
                List<ImmutableItemStack> items = new ArrayList<>();
                for (Tag item : tags) ImmutableItemStack.parse(provider, item).ifPresent(items::add);
                return Optional.of(items);
            });
        }
        <T> Field<List<T>> list(String name, Function<Decoder, Optional<T>> decoder) {
            return field(name, value -> {
                if (!(value instanceof ListTag tags) || tags.getElementType() != Tag.TAG_COMPOUND)
                    return Optional.empty();
                List<T> results = new ArrayList<>();
                for (int i = 0; i < tags.size(); i++)
                    decoder.apply(new Decoder(tags.getCompound(i), provider)).ifPresent(results::add);
                return Optional.of(results);
            });
        }
    }

    static {
        registerAll();
    }

    private static void registerAll() {
        register(OCComponentsJava.ID, d -> d.uuid(oc("uuid")).andRemove());
        register(() -> DataComponents.CUSTOM_NAME, d ->
            ItemUtils.getDisplayName(d.tag).isDefined()
                ? Optional.of(Component.literal(ItemUtils.getDisplayName(d.tag).get())) : Optional.empty());
        register(OCComponentsJava.OWNER, d -> first(
            () -> compose(c -> new Owner(c.require(d.string(oc("owner"))), c.require(d.uuid(oc("ownerUuid"))))),
            () -> compose(c -> new Owner(c.require(d.string("owner")), c.require(d.uuid("ownerUuid"))))));
        register(OCComponentsJava.LABEL, d -> first(
            () -> d.compound(oc("data"), nested -> nested.string(oc("label")).andRemove()),
            () -> d.compound(oc("data"), nested -> nested.string(oc("fs.label")).andRemove()),
            () -> d.string(oc("label")).andRemove(),
            () -> d.string(oc("fs.label")).andRemove()));
        register(OCComponentsJava.COMPONENTS, d -> first(
            () -> d.itemStackList(oc("components")).andRemove().map(Migrators::list),
            () -> d.itemStackList("components").andRemove().map(Migrators::list),
            () -> d.itemStackList(oc("disks")).andRemove().map(Migrators::list),
            () -> d.itemStackList("disks").andRemove().map(Migrators::list)));
        register(OCComponentsJava.CONTENTS, d -> d.itemStackList(oc("items")).andRemove().map(Migrators::list));
        register(OCComponentsJava.LOOT_DISK, d -> d.resourceLocation(oc("lootFactory")).andRemove());
        register(OCComponentsJava.DISK_COLOR, d -> d.intValue(oc("color"))
            .map(id -> Optional.of(DyeColor.byId(id))).andRemove());
        register(OCComponentsJava.CHARGE, d -> first(
            () -> d.doubleValue(oc("charge")).andRemove(), () -> d.doubleValue(oc("energy")).andRemove()));
        register(OCComponentsJava.MAX_CHARGE, d -> d.doubleValue(oc("maxEnergy")).andRemove());
        register(OCComponentsJava.UNMANAGED, d -> d.bool(oc("unmanaged")).andRemove());
        register(OCComponentsJava.LOCK, d -> d.string(oc("lock")).andRemove());
        register(OCComponentsJava.HEAD_POS, d -> d.intValue("headPos").andRemove());
        register(OCComponentsJava.EEPROM_CODE, d -> d.compound(oc("data"), nested ->
            nested.byteBuffer(oc("eeprom")).andRemove()));
        register(OCComponentsJava.EEPROM_DATA, d -> d.compound(oc("data"), nested ->
            nested.byteBuffer(oc("userdata")).andRemove()));
        register(OCComponentsJava.READONLY, d -> d.compound(oc("data"), nested ->
            nested.bool(oc("readonly")).andRemove()));
        register(OCComponentsJava.MF_COORD, d -> compose(c -> {
            ResourceLocation dimension = c.require(d.resourceLocation(oc("dimension")));
            int[] coordinates = c.require(d.intArray(oc("coord")).map(values ->
                values.length == 4 ? Optional.of(values) : Optional.empty()));
            return new MFCoords(dimension, new BlockPos(coordinates[0], coordinates[1], coordinates[2]),
                Direction.from3DDataValue(coordinates[3]));
        }));
        register(OCComponentsJava.SOURCE_MAP_ITEM, d -> d.itemStack(oc("map")).andRemove());
        register(OCComponentsJava.NANOMACHINES_NETWORK_INFO, d -> d.compound(oc("configuration"),
            nested -> Optional.of(nested.tag)));
        register(OCComponentsJava.ADDRESS, d -> d.compound("node", nested -> nested.string("address").andRemove()));
        register(OCComponentsJava.VISIBILITY, d -> first(
            () -> d.compound(oc("node"), nested -> visibility(nested, "visibility")),
            () -> d.compound(oc("node"), nested -> visibility(nested, oc("visibility"))),
            () -> d.compound("node", nested -> visibility(nested, "visibility")),
            () -> visibility(d, oc("visibility")), () -> visibility(d, "visibility")));
        register(OCComponentsJava.OPEN_PORTS, d -> d.intArray("openPorts").andRemove().map(Migrators::ints));
        register(OCComponentsJava.WAKE_MESSAGE, d -> compose(c -> new WakeMessage(
            c.require(d.string("wakeMessage")), c.require(d.bool("wakeMessageFuzzy")))));
        register(OCComponentsJava.TUNNEL, d -> d.string(oc("tunnel")).andRemove());
        register(OCComponentsJava.STRENGTH, d -> d.doubleValue("strength").andRemove());
        register(OCComponentsJava.TEXT_BUFFER, Migrators::textBuffer);
        register(OCComponentsJava.IS_ON, d -> d.bool(oc("isOn")).andRemove());
        register(OCComponentsJava.IS_POWERED, d -> first(
            () -> d.bool(oc("hasPower")).andRemove(), () -> d.bool("hasPower").andRemove()));
        register(OCComponentsJava.IS_PRECISE, d -> d.bool(oc("precise")).andRemove());
        register(OCComponentsJava.MAX_VIDEO_MODE, d -> compose(c -> new MaximumVideoMode(
            c.require(d.intValue(oc("maxWidth"))), c.require(d.intValue(oc("maxHeight"))),
            c.require(d.intValue(oc("maxDepth"))))));
        register(OCComponentsJava.VIDEO_MODE, d -> first(
            () -> compose(c -> new VideoMode(c.require(d.intValue(oc("viewportWidth"))),
                c.require(d.intValue(oc("viewportHeight"))))),
            () -> compose(c -> new VideoMode(c.require(d.intValue(oc("configWidth"))),
                c.require(d.intValue(oc("configHeight")))))));
        register(OCComponentsJava.KEYS, d -> d.stringList(oc("keys")).andRemove().map(Migrators::list));
        register(OCComponentsJava.TERMINAL_REFERENCE, d -> compose(c -> new TerminalReference(
            c.require(d.string(oc("key"))), c.require(d.string(oc("server"))))));
        register(OCComponentsJava.IS_RUNNING, d -> d.bool(oc("isRunning")).andRemove());
        register(OCComponentsJava.IS_ERRORED, d -> d.bool(oc("hasErrored"))
            .map(value -> value ? Optional.of(BoxedUnit.UNIT) : Optional.empty()).andRemove());
        register(OCComponentsJava.USERS, d -> d.stringList(oc("users")).andRemove()
            .map(values -> set(new HashSet<>(values))));
        register(OCComponentsJava.MACHINE, d -> first(
            () -> machineData(d),
            () -> d.compound(oc("machine"), Migrators::machineData),
            () -> d.compound("machine", Migrators::machineData)));
        register(OCComponentsJava.PRINT, Migrators::printData);
        register(OCComponentsJava.WAKE_THRESHOLD, d -> d.intValue("wakeThreshold").andRemove());
        register(OCComponentsJava.WIRELESS_REDSTONE_STATE, d -> compose(c ->
            new WirelessRedstoneState(c.require(d.intValue("wirelessFrequency")),
                c.require(d.bool("wirelessInput")), c.require(d.bool("wirelessOutput")))));
        register(OCComponentsJava.DRONE_STATE, d -> compose(c -> new DroneState(
            c.require(d.floatValue("targetX")), c.require(d.floatValue("targetY")),
            c.require(d.floatValue("targetZ")), c.require(d.floatValue("targetAcceleration")),
            c.require(d.byteValue("selectedSlot")), c.require(d.byteValue("selectedTank")))));
        register(OCComponentsJava.STATUS_TEXT, d -> d.string("statusText")
            .map(value -> Optional.of(Component.literal(value))).andRemove());
        register(OCComponentsJava.LIGHT_COLOR, d -> first(
            () -> d.intValue(oc("lightColor")).andRemove(),
            () -> d.intValue("lightColor").andRemove()).map(ColorRGBA::new));
        register(OCComponentsJava.ROBOT_CHARGE, d -> compose(c -> new RobotChargeInfo(
            c.require(d.intValue(oc("storedEnergy"))), c.require(d.intValue(oc("robotEnergy"))))));
        register(OCComponentsJava.CONTAINERS, d -> d.itemStackList(oc("containers")).andRemove().map(Migrators::list));
        register(OCComponentsJava.SELECTED_SLOT, d -> d.intValue(oc("selectedSlot")).andRemove());
        register(OCComponentsJava.SELECTED_TANK, d -> d.intValue(oc("selectedTank")).andRemove());
        register(OCComponentsJava.ROBOT_TOTAL_ANIMATION_TIME, d -> d.intValue(oc("animationTicksTotal")).andRemove());
        register(OCComponentsJava.ROBOT_CURRENT_ANIMATION, d -> compose(c -> {
            int ticks = c.require(d.intValue(oc("animationTicksLeft")));
            Field<Integer> x = d.intValue(oc("moveFromX"));
            Field<Integer> y = d.intValue(oc("moveFromY"));
            Field<Integer> z = d.intValue(oc("moveFromZ"));
            Optional<BlockPos> moveFrom = Optional.empty();
            if (x.value.isPresent() && y.value.isPresent() && z.value.isPresent())
                moveFrom = Optional.of(new BlockPos(c.require(x), c.require(y), c.require(z)));
            boolean swing = c.require(d.bool(oc("swingingTool")));
            byte axis = c.require(d.byteValue(oc("turnAxis")));
            return new RobotCurrentAnimation(ticks, option(moveFrom), swing, axis);
        }));
        register(OCComponentsJava.COMPOUND_DRIVER, d -> compose(c -> {
            long typeHash = c.require(d.longValue("typeHash"));
            CompoundTag remaining = c.remaining(d);
            Map<String, CompoundStorage> contents = new HashMap<>();
            for (String key : remaining.getAllKeys())
                contents.put(key, new CompoundStorage(remaining.getCompound(key)));
            return new Tuple2<>(typeHash, map(contents));
        }));
        register(OCComponentsJava.ATTACHMENT, d -> d.itemStack(oc("container")).andRemove());
        register(OCComponentsJava.COMPONENT_NODES, d -> d.list(oc("componentNodes"), child ->
            Optional.of(child.tag.isEmpty() ? Option$.MODULE$.empty() : Option$.MODULE$.apply(new CompoundStorage(child.tag))))
            .andRemove().map(Migrators::list));
        register(OCComponentsJava.FILESYSTEM_DATA, d -> first(
            () -> d.compound("fs", nested -> Optional.of(nested.tag)),
            () -> d.compound(oc("filesystem"), nested ->
                nested.compound("fs", fs -> Optional.of(fs.tag)))));
        register(OCComponentsJava.ROBOT_ROM_FILESYSTEM_DATA, d -> first(
            () -> d.compoundTag("romRobot").andRemove(),
            () -> d.compound(oc("robot"), nested -> nested.compoundTag("romRobot").andRemove())));
        register(OCComponentsJava.HANDLES, d -> first(
            () -> handles(d),
            () -> d.compound(oc("filesystem"), Migrators::handles)));
        register(OCComponentsJava.PALETTE, d -> d.intArray("palette").andRemove().map(values -> list(ints(values))));
        register(OCComponentsJava.RENDER_COLOR, d -> first(
            () -> d.intValue(oc("renderColorRGB")).andRemove().map(ColorRGBA::new),
            () -> d.intValue(oc("renderColor")).andRemove()
                .map(id -> new ColorRGBA(Color.rgbValues(DyeColor.byId(id))))));
        register(OCComponentsJava.HAS_REDSTONE_INPUT, d -> d.bool(oc("hadRedstoneInput")).andRemove());
        register(OCComponentsJava.INVERT_TOUCH, d -> d.bool(oc("invertTouchMode")).andRemove()
            .flatMap(value -> value ? Optional.of(BoxedUnit.UNIT) : Optional.empty()));
        register(OCComponentsJava.CHARGE_SPEED, d -> first(
            () -> d.doubleValue(oc("chargeSpeed")).andRemove(),
            () -> d.doubleValue("chargeSpeed").andRemove()));
        register(OCComponentsJava.INVERT_SIGNAL, d -> first(
            () -> d.bool(oc("invertSignal")).andRemove(),
            () -> d.bool("invertSignal").andRemove())
            .flatMap(value -> value ? Optional.of(BoxedUnit.UNIT) : Optional.empty()));
        register(OCComponentsJava.PRESENCE, d -> d.byteBuffer(oc("presence")).andRemove());
        register(OCComponentsJava.RACK_DATA, d -> d.list(oc("lastData"), child ->
            Optional.of(child.tag.isEmpty() ? Option$.MODULE$.empty() : Option$.MODULE$.apply(new CompoundStorage(child.tag))))
            .andRemove().map(Migrators::list));
        register(OCComponentsJava.RACK_NODE_MAPPING, Migrators::rackNodeMapping);
        register(OCComponentsJava.GRAPHICS_CARD, d -> compose(c ->
            new GraphicsCardState(option(c.optional(d.string("screen"))), c.require(d.intValue("bufferIndex")))));
        register(OCComponentsJava.VIDEO_RAM, d -> d.compound("videoRam", nested ->
            nested.list("pages", page -> composeValue(c -> new Tuple2<>(
                c.require(page.intValue("page_idx")),
                new CompoundStorage(c.require(page.compoundTag("page_data"))))))
                .andRemove().map(Migrators::list)));
        register(OCComponentsJava.LEASHED_ENTITIES, d -> d.stringList("leashedEntities").andRemove()
            .map(values -> list(values.stream().map(UUID::fromString).toList())));
        register(OCComponentsJava.TANK, d -> FluidStack.parse(d.provider, d.tag)
            .map(ImmutableFluidStack::copyOf));
        register(OCComponentsJava.FUEL_INVENTORY, d -> d.itemStack("inventory").andRemove());
        register(OCComponentsJava.FUEL_TICKS_REMAINING, d -> d.intValue("remainingTicks").andRemove());
        register(OCComponentsJava.DEBUG_CARD_ACCESS_CONTEXT, d -> compose(c -> new DebugCardAccessContext(
            c.require(d.string(oc("player"))), c.require(d.string(oc("accessNonce"))))));
        register(OCComponentsJava.DEBUG_CARD_REMOTE_NODE_POSITION, d -> compose(c -> new BlockPos(
            c.require(d.intValue(oc("remoteX"))), c.require(d.intValue(oc("remoteY"))),
            c.require(d.intValue(oc("remoteZ"))))));
    }

    private static List<Integer> ints(int[] values) {
        List<Integer> result = new ArrayList<>(values.length);
        for (int value : values) result.add(value);
        return result;
    }

    private static Optional<Visibility> visibility(Decoder d, String name) {
        return d.intValue(name).map(index -> index >= 0 && index < Visibility.values().length
            ? Optional.of(Visibility.values()[index]) : Optional.empty()).andRemove();
    }

    private static Optional<TextBufferContents> textBuffer(Decoder d) {
        return compose(c -> {
            int width = c.require(d.intValue("width").orElse(0));
            int height = c.require(d.intValue("height").orElse(0));
            List<String> buffer = c.require(d.stringList("buffer"));
            int depth = c.require(d.intValue("depth").orElse(0));
            int foreground = c.require(d.intValue("foreground").orElse(0));
            boolean foregroundPalette = c.require(d.bool("foregroundIsPalette").orElse(false));
            int background = c.require(d.intValue("background").orElse(0));
            boolean backgroundPalette = c.require(d.bool("backgroundIsPalette").orElse(false));
            int[][] colors = new int[height][width];
            boolean modern;
            try { modern = NbtDataStream.getIntArray(d.tag, "colors", colors, width, height); }
            catch (IOException error) { throw new IllegalStateException(error); }
            if (modern) c.require(d.tag("colors"));
            else {
                NbtDataStream.getIntArrayLegacy(d.tag, "color", colors, width, height);
                c.require(d.tag("color"));
            }
            return new TextBufferContents(width, height, depth, foreground, foregroundPalette,
                background, backgroundPalette, buffer, new TextBufferContents.IntArray(colors));
        });
    }

    private static Optional<MachineData> machineData(Decoder d) {
        return compose(c -> {
            int[] legacyStates = c.require(d.intArray("state"));
            List<MachineState> states = new ArrayList<>();
            for (int index : legacyStates)
                if (index >= 0 && index < MachineState.values().length) states.add(MachineState.values()[index]);
            Set<String> users = new HashSet<>(c.require(d.stringList("users").orElse(List.of())));
            Optional<String> message = c.optional(d.string("message"));
            List<MachineData.Component> components = c.require(d.list("components", component ->
                composeValue(inner -> new MachineData.Component(
                    inner.require(component.string("address")), inner.require(component.string("name"))))));
            List<MachineData.Signal> signals = c.require(d.list("signals", signal -> composeValue(inner -> {
                String name = inner.require(signal.string("name"));
                List<MachineData.Signal.Value> args = signal.compound("args", argData -> composeValue(argsComposition -> {
                    int count = argsComposition.require(argData.intValue("length"));
                    List<MachineData.Signal.Value> values = new ArrayList<>();
                    for (int i = 0; i < count; i++)
                        values.add(argsComposition.require(argData.field("arg" + i, Migrators::legacySignalValue)));
                    return values;
                })).orElseThrow(Missing::new);
                return new MachineData.Signal(name, args);
            })));
            long uptime = c.require(d.longValue("uptime").orElse(0L));
            long cpuTotal = c.require(d.longValue("cpuTotal").orElse(0L));
            int remainingPause = c.require(d.intValue("remainingPause").orElse(0));
            CompoundTag architecture = c.remaining(d);
            String oldAddress = architecture.getCompound("node").getString("address");
            Optional<String> address = oldAddress.isEmpty() ? Optional.empty() : Optional.of(oldAddress);
            architecture.remove("node");
            return new MachineData(states, users, message, components, address, architecture,
                signals, uptime, cpuTotal, remainingPause);
        });
    }

    private static Optional<MachineData.Signal.Value> legacySignalValue(Tag tag) {
        if (tag instanceof ByteTag value) return Optional.of(value.getAsByte() == -1
            ? MachineData.Signal.Null.INSTANCE : new MachineData.Signal.BooleanValue(value.getAsByte() == 1));
        if (tag instanceof LongTag value) return Optional.of(new MachineData.Signal.LongValue(value.getAsLong()));
        if (tag instanceof DoubleTag value) return Optional.of(new MachineData.Signal.DoubleValue(value.getAsDouble()));
        if (tag instanceof StringTag value) return Optional.of(new MachineData.Signal.StringValue(value.getAsString()));
        if (tag instanceof ByteArrayTag value)
            return Optional.of(new MachineData.Signal.ByteArray(ByteBuffer.wrap(value.getAsByteArray())));
        if (tag instanceof ListTag value) {
            Map<String, String> data = new HashMap<>();
            for (int i = 0; i < value.size(); i += 2)
                data.put(value.getString(i), value.getString(i + 1));
            return Optional.of(new MachineData.Signal.StringMap(data));
        }
        if (tag instanceof CompoundTag value) return Optional.of(new MachineData.Signal.Compound(value));
        return Optional.of(MachineData.Signal.Null.INSTANCE);
    }

    private static Optional<PrintData> printData(Decoder d) {
        return compose(c -> {
            Option<String> label = option(c.optional(d.string("label")));
            Option<String> tooltip = option(c.optional(d.string("tooltip")));
            boolean button = c.require(d.bool("isButtonMode").orElse(false));
            int redstone = first(
                () -> d.bool("emitRedstone").map(value -> value ? Optional.of(15) : Optional.empty()).andRemove(),
                () -> d.intValue("redstoneLevel").orElse(0).andRemove()).orElseThrow(Missing::new);
            boolean pressure = c.require(d.bool("pressurePlate").orElse(false));
            var off = c.require(shapes(d, "stateOff"));
            var on = c.require(shapes(d, "stateOn"));
            boolean beacon = c.require(d.bool("isBeaconBase").orElse(false));
            int light = c.require(d.intValue("lightLevel")
                .map(value -> value >= 0 && value <= 15 ? Optional.of(value) : Optional.empty()).orElse(0));
            boolean noclipOn = c.require(d.bool("noclipOn").orElse(false));
            boolean noclipOff = c.require(d.bool("noclipOff").orElse(false));
            return new PrintData(label, tooltip, button, redstone, pressure, off, on,
                beacon, light, noclipOff, noclipOn);
        });
    }

    private static Field<scala.collection.mutable.Set<li.cil.oc.common.item.data.PrintShape>> shapes(Decoder d, String name) {
        return d.field(name, value -> {
            if (!(value instanceof ListTag tags) || tags.getElementType() != Tag.TAG_COMPOUND)
                return Optional.empty();
            Set<li.cil.oc.common.item.data.PrintShape> shapes = new HashSet<>();
            for (int i = 0; i < tags.size(); i++) shapes.add(PrintDataSupport.nbtToShape(tags.getCompound(i)));
            return Optional.of(scala.collection.mutable.Set$.MODULE$.from(CollectionConverters.asScala(shapes)));
        });
    }

    private static Optional<scala.collection.immutable.Map<String, scala.collection.immutable.Set<Integer>>>
    handles(Decoder d) {
        return d.list("owners", child -> compose(c -> new Tuple2<>(
            c.require(child.string("address")), set(new HashSet<>(ints(c.require(child.intArray("handles"))))))))
            .andRemove().map(values -> {
                Map<String, scala.collection.immutable.Set<Integer>> handles = new HashMap<>();
                for (Tuple2<String, scala.collection.immutable.Set<Integer>> value : values)
                    handles.put(value._1(), value._2());
                return map(handles);
            });
    }

    private static Optional<scala.collection.immutable.List<scala.collection.immutable.List<Direction>>>
    rackNodeMapping(Decoder d) {
        return d.field(oc("nodeMapping"), value -> {
            if (!(value instanceof ListTag tags) || tags.getElementType() != Tag.TAG_INT_ARRAY)
                return Optional.empty();
            List<scala.collection.immutable.List<Direction>> directions = new ArrayList<>();
            for (Tag entry : tags) {
                List<Direction> row = new ArrayList<>();
                for (int side : ((IntArrayTag) entry).getAsIntArray()) row.add(Direction.from3DDataValue(side));
                directions.add(list(row));
            }
            return Optional.of(list(directions));
        }).andRemove();
    }
}
