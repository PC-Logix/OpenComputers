package li.cil.oc.util;

import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import scala.Tuple2;
import scala.Function1;
import scala.collection.immutable.Map$;
import scala.jdk.javaapi.CollectionConverters;

import java.util.*;

import com.google.common.base.Charsets;

/** Java implementation of typed map to NBT conversion. */
public final class ExtendedNBTJava {
    private ExtendedNBTJava() { }

    public static <A, B> scala.collection.Iterable<B> map(
            scala.collection.Iterable<A> values, Function1<A, B> convert) {
        scala.collection.mutable.ArrayBuffer<B> result = new scala.collection.mutable.ArrayBuffer<>();
        scala.collection.Iterator<A> iterator = values.iterator();
        while (iterator.hasNext()) result.append(convert.apply(iterator.next()));
        return result;
    }

    public static ByteTag toNbt(boolean value) { return ByteTag.valueOf(value); }
    public static ByteTag toNbt(byte value) { return ByteTag.valueOf(value); }
    public static ShortTag toNbt(short value) { return ShortTag.valueOf(value); }
    public static IntTag toNbt(int value) { return IntTag.valueOf(value); }
    public static LongTag toNbt(long value) { return LongTag.valueOf(value); }
    public static FloatTag toNbt(float value) { return FloatTag.valueOf(value); }
    public static DoubleTag toNbt(double value) { return DoubleTag.valueOf(value); }
    public static ByteArrayTag toNbt(byte[] value) { return new ByteArrayTag(value); }
    public static IntArrayTag toNbt(int[] value) { return new IntArrayTag(value); }
    public static ByteArrayTag toNbt(boolean[] value) {
        byte[] bytes = new byte[value.length];
        for (int i = 0; i < value.length; i++) bytes[i] = (byte) (value[i] ? 1 : 0);
        return new ByteArrayTag(bytes);
    }
    public static StringTag toNbt(String value) { return StringTag.valueOf(value); }

    public static CompoundTag toNbt(ItemStack value) {
        CompoundTag tag = new CompoundTag();
        if (value != null) value.save(ServerLifecycleHooks.getCurrentServer().registryAccess(), tag);
        return tag;
    }

    public static CompoundTag toNbt(Function1<CompoundTag, ?> writer) {
        CompoundTag tag = new CompoundTag();
        writer.apply(tag);
        return tag;
    }

    public static CompoundTag toNbt(scala.collection.immutable.Map<String, ?> values) {
        CompoundTag tag = new CompoundTag();
        scala.collection.Iterator<?> iterator = values.iterator();
        while (iterator.hasNext()) {
            Tuple2<?, ?> entry = (Tuple2<?, ?>) iterator.next();
            String key = (String) entry._1();
            Object value = entry._2();
            if (value instanceof Boolean item) tag.putBoolean(key, item);
            else if (value instanceof Byte item) tag.putByte(key, item);
            else if (value instanceof Short item) tag.putShort(key, item);
            else if (value instanceof Integer item) tag.putInt(key, item);
            else if (value instanceof Long item) tag.putLong(key, item);
            else if (value instanceof Float item) tag.putFloat(key, item);
            else if (value instanceof Double item) tag.putDouble(key, item);
            else if (value instanceof byte[] item) tag.putByteArray(key, item);
            else if (value instanceof int[] item) tag.putIntArray(key, item);
            else if (value instanceof String item) tag.putString(key, item);
            else if (value instanceof ItemStack item) tag.put(key, toNbt(item));
            else if (value instanceof Tag item) tag.put(key, item);
        }
        return tag;
    }

    public static scala.collection.immutable.Map<String, Object> toTypedMap(Tag tag) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", tag.getId());
        Object value;
        if (tag instanceof ByteTag byteTag) value = byteTag.getAsByte();
        else if (tag instanceof ShortTag shortTag) value = shortTag.getAsShort();
        else if (tag instanceof IntTag intTag) value = intTag.getAsInt();
        else if (tag instanceof LongTag longTag) value = longTag.getAsLong();
        else if (tag instanceof FloatTag floatTag) value = floatTag.getAsFloat();
        else if (tag instanceof DoubleTag doubleTag) value = doubleTag.getAsDouble();
        else if (tag instanceof ByteArrayTag byteArrayTag) value = byteArrayTag.getAsByteArray();
        else if (tag instanceof StringTag stringTag) value = stringTag.getAsString();
        else if (tag instanceof ListTag listTag) {
            List<scala.collection.immutable.Map<String, Object>> values = new ArrayList<>(listTag.size());
            for (int i = 0; i < listTag.size(); i++) values.add(toTypedMap(listTag.get(i)));
            value = CollectionConverters.asScala(values).toVector();
        } else if (tag instanceof CompoundTag compoundTag) {
            Map<String, scala.collection.immutable.Map<String, Object>> values = new LinkedHashMap<>();
            for (String key : compoundTag.getAllKeys()) values.put(key, toTypedMap(compoundTag.get(key)));
            value = Map$.MODULE$.from(CollectionConverters.asScala(values));
        } else if (tag instanceof IntArrayTag intArrayTag) value = intArrayTag.getAsIntArray();
        else throw new IllegalArgumentException();
        result.put("value", value);
        return Map$.MODULE$.from(CollectionConverters.asScala(result));
    }

    public static void appendNewCompoundTag(ListTag list, Function1<CompoundTag, ?> writer) {
        CompoundTag tag = new CompoundTag();
        writer.apply(tag);
        list.add(tag);
    }

    public static void append(ListTag list, scala.collection.Iterable<Tag> values) {
        scala.collection.Iterator<Tag> iterator = values.iterator();
        while (iterator.hasNext()) list.add(iterator.next());
    }

    public static <T extends Tag> void foreach(ListTag list, Function1<T, ?> callback) {
        for (int i = 0; i < list.size(); i++) callback.apply((T) list.get(i));
    }

    public static <T extends Tag, V> scala.collection.immutable.Vector<V> map(
            ListTag list, Function1<T, V> callback) {
        scala.collection.mutable.ArrayBuffer<V> values = new scala.collection.mutable.ArrayBuffer<>();
        for (int i = 0; i < list.size(); i++) values.append(callback.apply((T) list.get(i)));
        return values.toVector();
    }

    public static Tag typedMapToNbt(Object map) {
        Map<?, ?> typeAndValue = asMap(map, "Illegal value.");
        Object nbtType = typeAndValue.get("type");
        Object nbtValue = typeAndValue.get("value");
        if (!(nbtType instanceof Number number)) {
            if (nbtType == null) throw new IllegalArgumentException("Missing NBT type.");
            throw new IllegalArgumentException("Illegal NBT type '" + nbtType + "'.");
        }

        return switch (number.intValue()) {
            case Tag.TAG_BYTE -> ByteTag.valueOf(requireNumber(nbtValue, "Illegal or missing value.").byteValue());
            case Tag.TAG_SHORT -> ShortTag.valueOf(requireNumber(nbtValue, "Illegal or missing value.").shortValue());
            case Tag.TAG_INT -> IntTag.valueOf(requireNumber(nbtValue, "Illegal or missing value.").intValue());
            case Tag.TAG_LONG -> LongTag.valueOf(requireNumber(nbtValue, "Illegal or missing value.").longValue());
            case Tag.TAG_FLOAT -> FloatTag.valueOf(requireNumber(nbtValue, "Illegal or missing value.").floatValue());
            case Tag.TAG_DOUBLE -> DoubleTag.valueOf(requireNumber(nbtValue, "Illegal or missing value.").doubleValue());
            case Tag.TAG_BYTE_ARRAY -> new ByteArrayTag(bytes(asList(nbtValue)));
            case Tag.TAG_STRING -> StringTag.valueOf(stringValue(nbtValue));
            case Tag.TAG_LIST -> {
                ListTag list = new ListTag();
                for (Object value : asList(nbtValue)) list.add(typedMapToNbt(asMap(value, "Illegal value.")));
                yield list;
            }
            case Tag.TAG_COMPOUND -> {
                CompoundTag compound = new CompoundTag();
                for (Map.Entry<?, ?> entry : asMap(nbtValue, "Illegal value.").entrySet()) {
                    try {
                        compound.put((String) entry.getKey(), typedMapToNbt(asMap(entry.getValue(), "Illegal value.")));
                    } catch (Throwable error) {
                        throw new IllegalArgumentException("Error converting entry '" + entry.getKey() + "': " + error.getMessage());
                    }
                }
                yield compound;
            }
            case Tag.TAG_INT_ARRAY -> new IntArrayTag(integers(asList(nbtValue)));
            default -> throw new IllegalArgumentException("Unsupported NBT type '" + number.intValue() + "'.");
        };
    }

    private static Number requireNumber(Object value, String message) {
        if (value instanceof Number number) return number;
        throw new IllegalArgumentException(message);
    }

    private static String stringValue(Object value) {
        if (value instanceof String string) return string;
        if (value instanceof byte[] bytes) return new String(bytes, Charsets.UTF_8);
        throw new IllegalArgumentException("Illegal or missing value.");
    }

    private static byte[] bytes(List<?> values) {
        byte[] result = new byte[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = requireNumber(values.get(i), "Illegal value.").byteValue();
        return result;
    }

    private static int[] integers(List<?> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = requireNumber(values.get(i), "Illegal value.").intValue();
        return result;
    }

    private static List<?> asList(Object value) {
        if (value instanceof Object[] array) return Arrays.asList(array);
        if (value instanceof byte[] array) {
            List<Byte> result = new ArrayList<>(array.length);
            for (byte item : array) result.add(item);
            return result;
        }
        if (value instanceof int[] array) {
            List<Integer> result = new ArrayList<>(array.length);
            for (int item : array) result.add(item);
            return result;
        }
        if (value instanceof scala.collection.Map<?, ?> map) return mapToList(map);
        if (value instanceof scala.collection.Iterable<?> iterable) {
            List<Object> result = new ArrayList<>();
            scala.collection.Iterator<?> iterator = iterable.iterator();
            while (iterator.hasNext()) result.add(iterator.next());
            return result;
        }
        if (value instanceof java.lang.Iterable<?> iterable) {
            List<Object> result = new ArrayList<>();
            for (Object item : iterable) result.add(item);
            return result;
        }
        if (value instanceof java.util.Map<?, ?> map) return mapToList(map);
        if (value instanceof String string) {
            byte[] array = string.getBytes(Charsets.UTF_8);
            List<Byte> result = new ArrayList<>(array.length);
            for (byte item : array) result.add(item);
            return result;
        }
        throw new IllegalArgumentException("Illegal or missing value.");
    }

    private static List<?> mapToList(scala.collection.Map<?, ?> map) {
        List<Map.Entry<?, ?>> entries = new ArrayList<>();
        scala.collection.Iterator<?> iterator = map.iterator();
        while (iterator.hasNext()) {
            Tuple2<?, ?> entry = (Tuple2<?, ?>) iterator.next();
            if (entry._1() instanceof Number number) entries.add(new AbstractMap.SimpleImmutableEntry<>(number, entry._2()));
        }
        entries.sort(Comparator.comparingInt(entry -> ((Number) entry.getKey()).intValue()));
        List<Object> result = new ArrayList<>(entries.size());
        for (Map.Entry<?, ?> entry : entries) result.add(entry.getValue());
        return result;
    }

    private static List<?> mapToList(java.util.Map<?, ?> map) {
        List<Map.Entry<?, ?>> entries = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof Number number) entries.add(new AbstractMap.SimpleImmutableEntry<>(number, entry.getValue()));
        }
        entries.sort(Comparator.comparingInt(entry -> ((Number) entry.getKey()).intValue()));
        List<Object> result = new ArrayList<>(entries.size());
        for (Map.Entry<?, ?> entry : entries) result.add(entry.getValue());
        return result;
    }

    private static Map<?, ?> asMap(Object value, String message) {
        if (value instanceof scala.collection.Map<?, ?> map) {
            Map<Object, Object> result = new LinkedHashMap<>();
            scala.collection.Iterator<?> iterator = map.iterator();
            while (iterator.hasNext()) {
                Tuple2<?, ?> entry = (Tuple2<?, ?>) iterator.next();
                result.put(entry._1(), entry._2());
            }
            return result;
        }
        if (value instanceof java.util.Map<?, ?> map) return map;
        throw new IllegalArgumentException(message);
    }
}
