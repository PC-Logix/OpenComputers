package li.cil.oc.util;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.junit.Test;

import java.util.Map;
import java.util.LinkedHashMap;
import scala.collection.immutable.Map$;
import scala.jdk.javaapi.CollectionConverters;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ExtendedNBTJavaTest {
    @Test
    public void convertsPrimitiveTags() {
        Tag tag = ExtendedNBTJava.typedMapToNbt(Map.of("type", Tag.TAG_INT, "value", 42));
        assertEquals(42, ((IntTag) tag).getAsInt());
    }

    @Test
    public void sortsNumericMapKeysWhenBuildingLists() {
        Map<String, Object> first = Map.of("type", Tag.TAG_INT, "value", 10);
        Map<String, Object> second = Map.of("type", Tag.TAG_INT, "value", 20);
        Map<Integer, Object> indexed = Map.of(1, second, 0, first);
        ListTag tag = (ListTag) ExtendedNBTJava.typedMapToNbt(
                Map.of("type", Tag.TAG_LIST, "value", indexed));
        assertEquals(10, tag.getInt(0));
        assertEquals(20, tag.getInt(1));
    }

    @Test
    public void preservesByteArrayValues() {
        byte[] expected = {1, -2, 3};
        ByteArrayTag tag = (ByteArrayTag) ExtendedNBTJava.typedMapToNbt(
                Map.of("type", Tag.TAG_BYTE_ARRAY, "value", expected));
        assertArrayEquals(expected, tag.getAsByteArray());
    }

    @Test
    public void roundTripsNestedTypedMaps() {
        CompoundTag expected = new CompoundTag();
        expected.putString("name", "opencomputers");
        ListTag entries = new ListTag();
        entries.add(StringTag.valueOf("first"));
        entries.add(StringTag.valueOf("second"));
        expected.put("entries", entries);

        assertEquals(expected, ExtendedNBTJava.typedMapToNbt(ExtendedNBTJava.toTypedMap(expected)));
    }

    @Test
    public void convertsBooleanArraysAndPrimitiveMaps() {
        assertArrayEquals(new byte[]{1, 0, 1}, ExtendedNBTJava.toNbt(new boolean[]{true, false, true}).getAsByteArray());

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("count", 3);
        values.put("enabled", true);
        values.put("label", "test");
        var scalaValues = Map$.MODULE$.from(CollectionConverters.asScala(values));
        CompoundTag tag = ExtendedNBTJava.toNbt(scalaValues);
        assertEquals(3, tag.getInt("count"));
        assertEquals(true, tag.getBoolean("enabled"));
        assertEquals("test", tag.getString("label"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsupportedTags() {
        ExtendedNBTJava.typedMapToNbt(Map.of("type", 42, "value", 0));
    }
}
