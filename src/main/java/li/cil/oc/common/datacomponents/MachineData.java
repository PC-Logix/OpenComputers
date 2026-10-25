package li.cil.oc.common.datacomponents;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc.server.machine.MachineState;
import net.minecraft.nbt.CompoundTag;
import scala.jdk.javaapi.CollectionConverters;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record MachineData(List<MachineState> state, Set<String> users, Optional<String> message,
                          List<Component> components, Optional<String> nodeAddress,
                          CompoundTag architectureData, List<Signal> signals,
                          long uptime, long cpuTotal, int remainingPause) {
    private static final Codec<List<MachineState>> STATE_CODEC = Codec.STRING.listOf().xmap(
        values -> values.stream().map(MachineState::valueOf).toList(),
        values -> values.stream().map(MachineState::name).toList());
    private static final Codec<Set<String>> USERS_CODEC = Codec.STRING.listOf().xmap(
        HashSet::new, ArrayList::new);

    public static final Codec<MachineData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        STATE_CODEC.fieldOf("state").forGetter(MachineData::state),
        USERS_CODEC.fieldOf("users").forGetter(MachineData::users),
        Codec.STRING.optionalFieldOf("message").forGetter(MachineData::message),
        Component.CODEC.listOf().fieldOf("components").forGetter(MachineData::components),
        Codec.STRING.optionalFieldOf("node_address").forGetter(MachineData::nodeAddress),
        CompoundTag.CODEC.fieldOf("architecture_data").forGetter(MachineData::architectureData),
        Signal.CODEC.listOf().fieldOf("signals").forGetter(MachineData::signals),
        Codec.LONG.fieldOf("uptime").forGetter(MachineData::uptime),
        Codec.LONG.fieldOf("cpu_total").forGetter(MachineData::cpuTotal),
        Codec.INT.fieldOf("remaining_pause").forGetter(MachineData::remainingPause)
    ).apply(instance, MachineData::new));

    public record Component(String address, String name) {
        public static final Codec<Component> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("address").forGetter(Component::address),
            Codec.STRING.fieldOf("name").forGetter(Component::name)
        ).apply(instance, Component::new));
    }

    public record Signal(String name, List<Value> args) {
        public static final Codec<Signal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Signal::name),
            Value.CODEC.listOf().fieldOf("args").forGetter(Signal::args)
        ).apply(instance, Signal::new));

        public sealed interface Value permits Null, BooleanValue, LongValue, DoubleValue,
                                              StringValue, ByteArray, StringMap, Compound {
            Codec<Value> CODEC = new Codec<>() {
                @Override public <T> DataResult<Pair<Value, T>> decode(DynamicOps<T> ops, T input) {
                    return ops.getMapValues(input).flatMap(entries -> {
                        var first = entries.findFirst();
                        if (first.isEmpty()) return DataResult.success(Pair.of(Null.INSTANCE, ops.empty()));
                        return ops.getStringValue(first.get().getFirst()).flatMap(type -> {
                            T encoded = first.get().getSecond();
                            return switch (type) {
                                case "Z" -> ops.getBooleanValue(encoded).map(v -> Pair.of(new BooleanValue(v), ops.empty()));
                                case "L" -> ops.getNumberValue(encoded).map(v -> Pair.of(new LongValue(v.longValue()), ops.empty()));
                                case "D" -> ops.getNumberValue(encoded).map(v -> Pair.of(new DoubleValue(v.doubleValue()), ops.empty()));
                                case "S" -> ops.getStringValue(encoded).map(v -> Pair.of(new StringValue(v), ops.empty()));
                                case "B" -> ops.getByteBuffer(encoded).map(v -> Pair.of(new ByteArray(v), ops.empty()));
                                case "M" -> ops.getMapValues(encoded).flatMap(pairs -> {
                                    Map<String, String> values = new HashMap<>();
                                    for (var pair : pairs.toList()) {
                                        var key = ops.getStringValue(pair.getFirst()).result();
                                        var value = ops.getStringValue(pair.getSecond()).result();
                                        if (key.isEmpty() || value.isEmpty())
                                            return DataResult.error(() -> "Invalid string map entry");
                                        values.put(key.get(), value.get());
                                    }
                                    return DataResult.success(Pair.of(new StringMap(values), ops.empty()));
                                });
                                case "C" -> CompoundTag.CODEC.parse(ops, encoded)
                                    .map(v -> Pair.of(new Compound(v), ops.empty()));
                                default -> DataResult.error(() -> "Illegal value type " + type);
                            };
                        });
                    });
                }

                @Override public <T> DataResult<T> encode(Value input, DynamicOps<T> ops, T prefix) {
                    Map<T, T> encoded = new HashMap<>();
                    if (input instanceof BooleanValue value)
                        encoded.put(ops.createString("Z"), ops.createBoolean(value.value()));
                    else if (input instanceof LongValue value)
                        encoded.put(ops.createString("L"), ops.createLong(value.value()));
                    else if (input instanceof DoubleValue value)
                        encoded.put(ops.createString("D"), ops.createDouble(value.value()));
                    else if (input instanceof StringValue value)
                        encoded.put(ops.createString("S"), ops.createString(value.value()));
                    else if (input instanceof ByteArray value)
                        encoded.put(ops.createString("B"), ops.createByteList(value.value()));
                    else if (input instanceof StringMap value) {
                        Map<T, T> strings = new HashMap<>();
                        value.value().forEach((key, item) -> strings.put(ops.createString(key), ops.createString(item)));
                        encoded.put(ops.createString("M"), ops.createMap(strings));
                    } else if (input instanceof Compound value) {
                        return CompoundTag.CODEC.encodeStart(ops, value.value()).map(tag ->
                            ops.createMap(Map.of(ops.createString("C"), tag)));
                    }
                    return DataResult.success(ops.createMap(encoded));
                }
            };
        }

        public enum Null implements Value { INSTANCE }
        public record BooleanValue(boolean value) implements Value { }
        public record LongValue(long value) implements Value { }
        public record DoubleValue(double value) implements Value { }
        public record StringValue(String value) implements Value { }
        public record ByteArray(ByteBuffer value) implements Value { }
        public record StringMap(Map<String, String> value) implements Value { }
        public record Compound(CompoundTag value) implements Value { }
    }

    public static Object[] signalArgs(Signal signal) {
        Object[] args = new Object[signal.args().size()];
        for (int i = 0; i < args.length; i++) {
            Signal.Value value = signal.args().get(i);
            if (value instanceof Signal.BooleanValue v) args[i] = v.value();
            else if (value instanceof Signal.LongValue v) args[i] = v.value();
            else if (value instanceof Signal.DoubleValue v) args[i] = v.value();
            else if (value instanceof Signal.StringValue v) args[i] = v.value();
            else if (value instanceof Signal.ByteArray v) {
                ByteBuffer bytes = v.value();
                if (bytes.hasArray()) args[i] = bytes.array();
                else {
                    byte[] data = new byte[bytes.remaining()];
                    bytes.get(data);
                    args[i] = data;
                }
            } else if (value instanceof Signal.StringMap v) args[i] = CollectionConverters.asScala(new HashMap<>(v.value()));
            else if (value instanceof Signal.Compound v) args[i] = v.value();
        }
        return args;
    }

    public static Signal signal(String name, Object[] args) {
        List<Signal.Value> values = new ArrayList<>();
        for (Object value : args) {
            if (value instanceof java.lang.Boolean v) values.add(new Signal.BooleanValue(v));
            else if (value instanceof java.lang.Long v) values.add(new Signal.LongValue(v));
            else if (value instanceof java.lang.Double v) values.add(new Signal.DoubleValue(v));
            else if (value instanceof String v) values.add(new Signal.StringValue(v));
            else if (value instanceof byte[] v) values.add(new Signal.ByteArray(ByteBuffer.wrap(v)));
            else if (value instanceof scala.collection.Map<?, ?> map) {
                Map<String, String> strings = new HashMap<>();
                CollectionConverters.asJava(map).forEach((key, item) ->
                    strings.put(String.valueOf(key), String.valueOf(item)));
                values.add(new Signal.StringMap(strings));
            } else if (value instanceof CompoundTag v) values.add(new Signal.Compound(v));
            else values.add(Signal.Null.INSTANCE);
        }
        return new Signal(name, values);
    }
}
