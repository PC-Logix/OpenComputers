package li.cil.oc.integration.minecraft;

import li.cil.oc.api.driver.Converter;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

public final class ConverterNBT implements Converter {
    public static final ConverterNBT INSTANCE = new ConverterNBT();

    private ConverterNBT() {}

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof CompoundTag tag) {
            output.put("oc:flatten", flatten(tag));
        }
    }

    private Object flatten(Tag tag) {
        if (tag instanceof ByteTag value) return value.getAsByte();
        if (tag instanceof ShortTag value) return value.getAsShort();
        if (tag instanceof IntTag value) return value.getAsInt();
        if (tag instanceof LongTag value) return value.getAsLong();
        if (tag instanceof FloatTag value) return value.getAsFloat();
        if (tag instanceof DoubleTag value) return value.getAsDouble();
        if (tag instanceof ByteArrayTag value) return value.getAsByteArray();
        if (tag instanceof StringTag value) return value.getAsString();
        if (tag instanceof ListTag value) {
            Object[] result = new Object[value.size()];
            for (int i = 0; i < value.size(); i++) result[i] = flatten(value.get(i));
            return result;
        }
        if (tag instanceof CompoundTag value) {
            Map<String, Object> result = new HashMap<>();
            for (String key : value.getAllKeys()) result.put(key, flatten(value.get(key)));
            return result;
        }
        if (tag instanceof IntArrayTag value) return value.getAsIntArray();
        throw new IllegalArgumentException("Unsupported NBT tag: " + tag.getClass().getName());
    }
}
