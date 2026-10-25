package li.cil.oc.server.network;

import li.cil.oc.util.ResultWrapper;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import scala.None$;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NetworkPacketDataTest {
    @Test
    public void sizePreservesTheNetworkPayloadAccountingRules() {
        Object[] values = {
                null, ResultWrapper.unit, None$.MODULE$, Boolean.TRUE, Byte.valueOf((byte) 1), Short.valueOf((short) 2),
                Integer.valueOf(3), Long.valueOf(4), Float.valueOf(5), Double.valueOf(6), "x", new byte[]{7, 8}
        };

        assertEquals(68, NetworkPacketData.size(values, values.length));
    }

    @Test(expected = IllegalArgumentException.class)
    public void sizeRejectsPayloadsOverThePartLimit() {
        NetworkPacketData.size(new Object[]{1, 2}, 1);
    }

    @Test
    public void saveDataPreservesSupportedValuesAndLeavesEmptySlotsAbsent() {
        CompoundTag tag = new CompoundTag();
        Object[] values = {null, Boolean.TRUE, Byte.valueOf((byte) 4), "x", new byte[]{5, 6}, ResultWrapper.unit, None$.MODULE$};

        NetworkPacketData.saveData("source", null, 17, values, 3, tag);

        assertEquals("source", tag.getString("source"));
        assertFalse(tag.contains("dest"));
        assertEquals(17, tag.getInt("port"));
        assertEquals(3, tag.getInt("ttl"));
        assertEquals(values.length, tag.getInt("dataLength"));
        assertFalse(tag.contains("data0"));
        assertTrue(tag.getBoolean("data1"));
        assertEquals(4, tag.getShort("data2"));
        assertEquals("x", tag.getString("data3"));
        assertArrayEquals(new byte[]{5, 6}, tag.getByteArray("data4"));
        assertFalse(tag.contains("data5"));
        assertFalse(tag.contains("data6"));
    }
}