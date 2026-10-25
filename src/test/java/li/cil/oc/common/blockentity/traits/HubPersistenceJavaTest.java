package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Packet;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;

import static org.junit.Assert.assertEquals;

public final class HubPersistenceJavaTest {
    @Test
    public void savesPlugAndQueueListsAndOnlyWritesActiveCooldown() {
        Queue<HubPacketEntry> queue = new ArrayDeque<>();
        CompoundTag nbt = new CompoundTag();

        HubPersistenceJava.save(nbt, null, "plugs", "queue", "side", "cooldown",
            new li.cil.oc.api.network.Node[0], queue, 4);

        assertEquals(0, nbt.getList("plugs", 10).size());
        assertEquals(0, nbt.getList("queue", 10).size());
        assertEquals(4, nbt.getInt("cooldown"));

        HubPersistenceJava.save(nbt, null, "plugs", "queue", "side", "cooldown",
            new li.cil.oc.api.network.Node[0], queue, -1);
        assertEquals(4, nbt.getInt("cooldown"));
    }

    @Test
    public void loadingEmptyPlugAndQueueListsPreservesCooldownWhenTagIsAbsent() throws Exception {
        Queue<HubPacketEntry> queue = new ArrayDeque<>();
        CompoundTag nbt = new CompoundTag();
        nbt.put("plugs", new ListTag());
        nbt.put("queue", new ListTag());

        int cooldown = HubPersistenceJava.load(nbt, null, "plugs", "queue", "side", "cooldown",
            new li.cil.oc.api.network.Node[0], queue, 2);

        assertEquals(2, cooldown);
        assertEquals(0, queue.size());
    }

    @Test
    public void savesAbsentSourceSideAsTheExistingNoneSentinel() {
        Queue<HubPacketEntry> queue = new ArrayDeque<>();
        queue.add(new HubPacketEntry(null, new TestPacket()));
        CompoundTag nbt = new CompoundTag();

        HubPersistenceJava.save(nbt, null, "plugs", "queue", "side", "cooldown",
            new li.cil.oc.api.network.Node[0], queue, -1);

        CompoundTag saved = nbt.getList("queue", 10).getCompound(0);
        assertEquals((byte) -1, saved.getByte("side"));
        assertEquals("payload", saved.getString("payload"));
    }

    @Test
    public void optionalDirectionAccessPreservesTheExistingNbtEncoding() {
        CompoundTag nbt = new CompoundTag();
        li.cil.oc.util.ExtendedCompoundTag extended = new li.cil.oc.util.ExtendedCompoundTag(nbt);

        extended.setDirection("side", Optional.of(net.minecraft.core.Direction.WEST));
        assertEquals((byte) net.minecraft.core.Direction.WEST.ordinal(), nbt.getByte("side"));
        assertEquals(Optional.of(net.minecraft.core.Direction.WEST), extended.getDirectionOptional("side"));

        extended.setDirection("side", Optional.empty());
        assertEquals((byte) -1, nbt.getByte("side"));
        assertEquals(Optional.empty(), extended.getDirectionOptional("side"));
    }

    private static final class TestPacket implements Packet {
        @Override public String source() { return "source"; }
        @Override public String destination() { return null; }
        @Override public int port() { return 0; }
        @Override public Object[] data() { return new Object[0]; }
        @Override public int size() { return 0; }
        @Override public int ttl() { return 1; }
        @Override public Packet hop() { return this; }
        @Override public void saveData(CompoundTag nbt) { nbt.putString("payload", "payload"); }
    }
}
