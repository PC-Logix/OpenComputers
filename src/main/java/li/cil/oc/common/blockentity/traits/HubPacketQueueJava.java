package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Packet;
import li.cil.oc.util.MovingAverage;
import net.minecraft.core.Direction;

import java.util.Queue;
import java.util.Optional;
import java.util.function.Consumer;

/** Queue admission and cooldown processing for relay hosts. */
final class HubPacketQueueJava {
    private HubPacketQueueJava() { }

    static EnqueueResult tryEnqueue(Queue<HubPacketEntry> queue, Optional<Direction> sourceSide, Packet packet,
                                    int maxQueueSize, int relayCooldown, int relayDelay) {
        synchronized (queue) {
            if (packet.ttl() <= 0 || queue.size() >= maxQueueSize) {
                return new EnqueueResult(false, relayCooldown);
            }
            queue.add(new HubPacketEntry(sourceSide.orElse(null), packet.hop()));
            int nextCooldown = relayCooldown < 0 ? relayDelay - 1 : relayCooldown;
            return new EnqueueResult(true, nextCooldown);
        }
    }

    static int tick(Queue<HubPacketEntry> queue, int relayCooldown,
                    int relayAmount, int relayDelay, long gameTime, MovingAverage packetsPerCycle,
                    Consumer<HubPacketEntry> relay) {
        if (relayCooldown > 0) return relayCooldown - 1;

        int nextCooldown = -1;
        if (!queue.isEmpty()) {
            synchronized (queue) {
                int packetsToRelay = Math.min(queue.size(), relayAmount);
                packetsPerCycle.add(packetsToRelay);
                for (int i = 0; i < packetsToRelay; i++) relay.accept(queue.remove());
                if (!queue.isEmpty()) nextCooldown = relayDelay - 1;
            }
        } else if (gameTime % relayDelay == 0) {
            packetsPerCycle.add(0);
        }
        return nextCooldown;
    }

    static final class EnqueueResult {
        private final boolean enqueued;
        private final int relayCooldown;

        private EnqueueResult(boolean enqueued, int relayCooldown) {
            this.enqueued = enqueued;
            this.relayCooldown = relayCooldown;
        }

        boolean enqueued() { return enqueued; }
        int relayCooldown() { return relayCooldown; }
    }
}
