package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Packet;
import li.cil.oc.util.MovingAverage;
import net.minecraft.core.Direction;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class HubPacketQueueJavaTest {
    @Test
    public void admissionHopsPacketsAndHonorsQueueCapacityAndTtl() {
        Queue<HubPacketEntry> queue = new ArrayDeque<>();

        HubPacketQueueJava.EnqueueResult accepted = HubPacketQueueJava.tryEnqueue(
            queue, Optional.of(Direction.NORTH), new TestPacket(2, "first"), 1, -1, 4);
        assertTrue(accepted.enqueued());
        assertEquals(3, accepted.relayCooldown());
        assertEquals(1, queue.size());
        assertEquals(Direction.NORTH, queue.peek().sourceSide());
        assertEquals(1, queue.peek().packet().ttl());

        HubPacketQueueJava.EnqueueResult full = HubPacketQueueJava.tryEnqueue(
            queue, Optional.of(Direction.NORTH), new TestPacket(2, "second"), 1, accepted.relayCooldown(), 4);
        assertFalse(full.enqueued());
        assertEquals(3, full.relayCooldown());

        HubPacketQueueJava.EnqueueResult expired = HubPacketQueueJava.tryEnqueue(
            queue, Optional.empty(), new TestPacket(0, "expired"), 2, -1, 4);
        assertFalse(expired.enqueued());
        assertEquals(1, queue.size());
    }

    @Test
    public void tickDrainsFifoBatchesAndSchedulesTheNextRelayCycle() {
        Queue<HubPacketEntry> queue = new ArrayDeque<>();
        HubPacketQueueJava.tryEnqueue(queue, Optional.empty(), new TestPacket(3, "first"), 3, -1, 3);
        HubPacketQueueJava.tryEnqueue(queue, Optional.empty(), new TestPacket(3, "second"), 3, -1, 3);
        MovingAverage average = new MovingAverage(2);
        List<String> relayed = new ArrayList<>();

        int cooldown = HubPacketQueueJava.tick(queue, 2, 1, 3, 0, average,
            packet -> relayed.add((String) packet.packet().data()[0]));
        assertEquals(1, cooldown);
        assertTrue(relayed.isEmpty());

        cooldown = HubPacketQueueJava.tick(queue, 0, 1, 3, 0, average,
            packet -> relayed.add((String) packet.packet().data()[0]));
        assertEquals(2, cooldown);
        assertEquals(List.of("first"), relayed);
        assertEquals(1, queue.size());

        cooldown = HubPacketQueueJava.tick(queue, 0, 1, 3, 0, average,
            packet -> relayed.add((String) packet.packet().data()[0]));
        assertEquals(-1, cooldown);
        assertEquals(List.of("first", "second"), relayed);
        assertEquals(0, queue.size());
        assertEquals(1, average.get());
    }

    @Test
    public void nullableSourceSideIsRetainedForUnaddressedPackets() {
        Queue<HubPacketEntry> queue = new ArrayDeque<>();
        HubPacketQueueJava.tryEnqueue(queue, Optional.empty(), new TestPacket(3, "packet"), 2, -1, 3);
        assertEquals(Optional.empty(), queue.peek().sourceSideOptional());
    }

    private static final class TestPacket implements Packet {
        private final int ttl;
        private final String value;
        private TestPacket(int ttl, String value) { this.ttl = ttl; this.value = value; }
        @Override public String source() { return "source"; }
        @Override public String destination() { return null; }
        @Override public int port() { return 0; }
        @Override public Object[] data() { return new Object[]{value}; }
        @Override public int size() { return value.length(); }
        @Override public int ttl() { return ttl; }
        @Override public Packet hop() { return new TestPacket(Math.max(0, ttl - 1), value); }
        @Override public void saveData(net.minecraft.nbt.CompoundTag nbt) { }
    }
}
