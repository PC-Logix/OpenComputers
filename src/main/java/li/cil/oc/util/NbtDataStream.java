package li.cil.oc.util;

import net.minecraft.nbt.CompoundTag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class NbtDataStream {
    private NbtDataStream() {
    }

    public static boolean getShortArray(CompoundTag nbt, String key, short[][] array, int width, int height)
        throws IOException {
        if (!nbt.contains(key)) return false;

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(nbt.getByteArray(key)));
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (input.available() < Short.BYTES) return true;
                array[y][x] = input.readShort();
            }
        }
        return true;
    }

    public static boolean getIntArrayLegacy(CompoundTag nbt, String key, short[][] array, int width, int height) {
        if (!nbt.contains(key)) return false;

        int[] colors = nbt.getIntArray(key);
        for (int y = 0; y < height; y++) {
            short[] row = array[y];
            for (int x = 0; x < width; x++) {
                int index = x + y * width;
                if (index >= colors.length) return true;
                row[x] = (short) colors[index];
            }
        }
        return true;
    }

    public static void setShortArray(CompoundTag nbt, String key, short[] array) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        for (short value : array) output.writeShort(value);
        nbt.putByteArray(key, bytes.toByteArray());
    }

    public static boolean getOptBoolean(CompoundTag nbt, String key, boolean defaultValue) {
        return nbt.contains(key) ? nbt.getBoolean(key) : defaultValue;
    }

    public static String getOptString(CompoundTag nbt, String key, String defaultValue) {
        return nbt.contains(key) ? nbt.getString(key) : defaultValue;
    }

    public static CompoundTag getOptNbt(CompoundTag nbt, String key) {
        return nbt.contains(key) ? nbt.getCompound(key) : new CompoundTag();
    }

    public static int getOptInt(CompoundTag nbt, String key, int defaultValue) {
        return nbt.contains(key) ? nbt.getInt(key) : defaultValue;
    }
}
