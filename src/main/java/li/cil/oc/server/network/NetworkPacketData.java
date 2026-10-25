package li.cil.oc.server.network;

import li.cil.oc.OpenComputers;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import scala.None$;

/** Packet payload sizing and persistence shared by the network packet wrapper. */
public final class NetworkPacketData {
    private NetworkPacketData() {}

    public static int size(Object[] data, int maxParts) {
        if (data == null) return 0;
        if (data.length > maxParts) throw new IllegalArgumentException("packet has too many parts");

        int size = data.length * 2;
        for (Object value : data) {
            if (value == null || value == ResultWrapper.unit || value == None$.MODULE$) size += 4;
            else if (value instanceof Boolean) size += 1;
            else if (value instanceof Byte) size += 2; // Bytes are currently sent as shorts.
            else if (value instanceof Short) size += 2;
            else if (value instanceof Integer) size += 4;
            else if (value instanceof Long) size += 8;
            else if (value instanceof Float) size += 4;
            else if (value instanceof Double) size += 8;
            else if (value instanceof String string) size += Math.max(string.length(), 1);
            else if (value instanceof byte[] bytes) size += Math.max(bytes.length, 1);
            else throw new IllegalArgumentException("unsupported data type: " + value + " (" + value.getClass().getCanonicalName() + ")");
        }
        return size;
    }

    public static Object[] loadPayload(CompoundTag nbt) {
        int length = Math.max(0, nbt.getInt("dataLength"));
        Object[] data = new Object[length];
        for (int i = 0; i < length; i++) {
            String key = "data" + i;
            if (!nbt.contains(key)) continue;

            Tag tag = nbt.get(key);
            if (tag instanceof ByteTag value) data[i] = value.getAsByte() != 0;
            else if (tag instanceof ShortTag value) data[i] = value.getAsShort();
            else if (tag instanceof IntTag value) data[i] = value.getAsInt();
            else if (tag instanceof LongTag value) data[i] = value.getAsLong();
            else if (tag instanceof FloatTag value) data[i] = value.getAsFloat();
            else if (tag instanceof DoubleTag value) data[i] = value.getAsDouble();
            else if (tag instanceof StringTag value) data[i] = value.getAsString();
            else if (tag instanceof ByteArrayTag value) data[i] = value.getAsByteArray();
            else throw new scala.MatchError(tag);
        }
        return data;
    }

    public static void saveData(String source, String destination, int port, Object[] data, int ttl, CompoundTag nbt) {
        nbt.putString("source", source);
        if (destination != null && !destination.isEmpty()) nbt.putString("dest", destination);
        nbt.putInt("port", port);
        nbt.putInt("ttl", ttl);
        nbt.putInt("dataLength", data.length);

        for (int i = 0; i < data.length; i++) {
            Object value = data[i];
            String key = "data" + i;
            if (value == null || value == ResultWrapper.unit || value == None$.MODULE$) continue;
            if (value instanceof Boolean bool) nbt.putBoolean(key, bool);
            else if (value instanceof Byte number) nbt.putShort(key, number.shortValue());
            else if (value instanceof Short number) nbt.putShort(key, number);
            else if (value instanceof Integer number) nbt.putInt(key, number);
            else if (value instanceof Long number) nbt.putLong(key, number);
            else if (value instanceof Float number) nbt.putFloat(key, number);
            else if (value instanceof Double number) nbt.putDouble(key, number);
            else if (value instanceof String string) nbt.putString(key, string);
            else if (value instanceof byte[] bytes) nbt.putByteArray(key, bytes);
            else OpenComputers.log().warn("Unexpected type while saving network packet: " + value.getClass().getName());
        }
    }
}
