package li.cil.oc.server.machine;

import com.google.common.base.Charsets;
import li.cil.oc.api.machine.Arguments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import scala.collection.Map;
import scala.jdk.javaapi.CollectionConverters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/** Java implementation of the shared argument conversion rules. */
public final class ArgumentsImpl implements Arguments {
    private final Object[] args;

    public ArgumentsImpl(Object... args) {
        this.args = args.clone();
    }

    public ArgumentsImpl(scala.collection.immutable.Seq<?> args) {
        this.args = scala.jdk.javaapi.CollectionConverters.asJava(args).toArray();
    }

    @Override
    public Iterator<Object> iterator() {
        return Arrays.asList(args).iterator();
    }

    @Override
    public int count() {
        return args.length;
    }

    @Override
    public Object checkAny(int index) {
        checkIndex(index, "value");
        Object value = args[index];
        return value == scala.runtime.BoxedUnit.UNIT || value == scala.None$.MODULE$ ? null : value;
    }

    @Override public Object optAny(int index, Object def) { return isDefined(index) ? checkAny(index) : def; }

    @Override
    public boolean checkBoolean(int index) {
        checkIndex(index, "boolean");
        if (args[index] instanceof Boolean value) return value;
        throw typeError(index, args[index], "boolean");
    }

    @Override public boolean optBoolean(int index, boolean def) { return isDefined(index) ? checkBoolean(index) : def; }

    @Override
    public double checkDouble(int index) {
        checkIndex(index, "number");
        if (args[index] instanceof Number value) return value.doubleValue();
        throw typeError(index, args[index], "number");
    }

    @Override public double optDouble(int index, double def) { return isDefined(index) ? checkDouble(index) : def; }

    @Override
    public int checkInteger(int index) {
        checkIndex(index, "integer");
        Object value = args[index];
        if (value instanceof Double number) {
            if (number.isNaN()) throw intError(index, value);
            if (number > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            if (number < Integer.MIN_VALUE) return Integer.MIN_VALUE;
            return number.intValue();
        }
        if (value instanceof Float number) {
            if (number.isNaN()) throw intError(index, value);
            if (number > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            if (number < Integer.MIN_VALUE) return Integer.MIN_VALUE;
            return number.intValue();
        }
        if (value instanceof Long number) {
            if (number > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            if (number < Integer.MIN_VALUE) return Integer.MIN_VALUE;
            return number.intValue();
        }
        if (value instanceof Number number) return number.intValue();
        throw typeError(index, value, "integer");
    }

    @Override public int optInteger(int index, int def) { return isDefined(index) ? checkInteger(index) : def; }

    @Override
    public long checkLong(int index) {
        checkIndex(index, "integer");
        Object value = args[index];
        if (value instanceof Double number) {
            if (number.isNaN()) throw intError(index, value);
            if (number > Long.MAX_VALUE) return Long.MAX_VALUE;
            if (number < Long.MIN_VALUE) return Long.MIN_VALUE;
            return number.longValue();
        }
        if (value instanceof Float number) {
            if (number.isNaN()) throw intError(index, value);
            if (number > Long.MAX_VALUE) return Long.MAX_VALUE;
            if (number < Long.MIN_VALUE) return Long.MIN_VALUE;
            return number.longValue();
        }
        if (value instanceof Number number) return number.longValue();
        throw typeError(index, value, "integer");
    }

    @Override public long optLong(int index, long def) { return isDefined(index) ? checkLong(index) : def; }

    @Override
    public String checkString(int index) {
        checkIndex(index, "string");
        if (args[index] instanceof String value) return value;
        if (args[index] instanceof byte[] value) return new String(value, Charsets.UTF_8);
        throw typeError(index, args[index], "string");
    }

    @Override public String optString(int index, String def) { return isDefined(index) ? checkString(index) : def; }

    @Override
    public byte[] checkByteArray(int index) {
        checkIndex(index, "string");
        if (args[index] instanceof String value) return value.getBytes(Charsets.UTF_8);
        if (args[index] instanceof byte[] value) return value;
        throw typeError(index, args[index], "string");
    }

    @Override public byte[] optByteArray(int index, byte[] def) { return isDefined(index) ? checkByteArray(index) : def; }

    @Override
    public java.util.Map checkTable(int index) {
        checkIndex(index, "table");
        Object value = args[index];
        if (value instanceof java.util.Map<?, ?> map) return map;
        if (value instanceof Map<?, ?> map) return CollectionConverters.asJava(map);
        throw typeError(index, value, "table");
    }

    @Override public java.util.Map optTable(int index, java.util.Map def) { return isDefined(index) ? checkTable(index) : def; }

    @Override
    public ItemStack checkItemStack(int index) {
        java.util.Map map = checkTable(index);
        Object name = map.get("name");
        if (!(name instanceof String itemName)) throw new IllegalArgumentException("invalid item stack");
        Object damageValue = map.get("damage");
        int damage = damageValue instanceof Number number ? number.intValue() : 0;
        Object tagValue = map.get("tag");
        CompoundTag tag = null;
        if (tagValue instanceof byte[] bytes) tag = toNbtTagCompound(bytes);
        else if (tagValue instanceof String string) tag = toNbtTagCompound(string.getBytes(Charsets.UTF_8));

        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemName));
        if (item == null) throw new IllegalArgumentException("invalid item stack");
        ItemStack stack = new ItemStack(item, 1);
        stack.setDamageValue(damage);
        if (tag != null) CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        return stack;
    }

    @Override public ItemStack optItemStack(int index, ItemStack def) { return isDefined(index) ? checkItemStack(index) : def; }

    @Override public boolean isBoolean(int index) { return isDefined(index) && args[index] instanceof Boolean; }
    @Override public boolean isDouble(int index) { return isDefined(index) && args[index] instanceof Number; }
    @Override public boolean isInteger(int index) { return isIntegerLike(index); }
    @Override public boolean isLong(int index) { return isIntegerLike(index); }
    @Override public boolean isString(int index) { return isDefined(index) && (args[index] instanceof String || args[index] instanceof byte[]); }
    @Override public boolean isByteArray(int index) { return isString(index); }
    @Override public boolean isTable(int index) { return isDefined(index) && (args[index] instanceof java.util.Map<?, ?> || args[index] instanceof Map<?, ?>); }

    @Override
    public boolean isItemStack(int index) {
        if (!isTable(index)) return false;
        Object name = checkTable(index).get("name");
        return name instanceof String || name instanceof byte[];
    }

    @Override
    public Object[] toArray() {
        Object[] result = args.clone();
        for (int i = 0; i < result.length; i++) if (result[i] instanceof byte[] bytes) result[i] = new String(bytes, Charsets.UTF_8);
        return result;
    }

    private boolean isIntegerLike(int index) {
        if (!isDefined(index)) return false;
        Object value = args[index];
        if (value instanceof Double number) return !number.isNaN();
        if (value instanceof Float number) return !number.isNaN();
        return value instanceof Number;
    }

    private boolean isDefined(int index) { return index >= 0 && index < args.length && args[index] != null; }

    private void checkIndex(int index, String name) {
        if (index < 0) throw new IndexOutOfBoundsException();
        if (args.length <= index) throw new IllegalArgumentException("bad arguments #" + (index + 1) + " (" + name + " expected, got no value)");
    }

    private IllegalArgumentException typeError(int index, Object have, String want) {
        return new IllegalArgumentException("bad argument #" + (index + 1) + " (" + want + " expected, got " + typeName(have) + ")");
    }

    private IllegalArgumentException intError(int index, Object have) {
        return new IllegalArgumentException("bad argument #" + (index + 1) + " (" + typeName(have) + " has no integer representation)");
    }

    private String typeName(Object value) {
        if (value == null || value == scala.runtime.BoxedUnit.UNIT || value == scala.None$.MODULE$) return "nil";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) return "integer";
        if (value instanceof Number) return "number";
        if (value instanceof String || value instanceof byte[]) return "string";
        if (value instanceof java.util.Map<?, ?> || value instanceof Map<?, ?>) return "table";
        return value.getClass().getSimpleName();
    }

    private CompoundTag toNbtTagCompound(byte[] data) {
        try {
            return NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
