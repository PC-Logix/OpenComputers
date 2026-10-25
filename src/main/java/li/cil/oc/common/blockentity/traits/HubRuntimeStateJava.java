package li.cil.oc.common.blockentity.traits;

import li.cil.oc.util.MovingAverage;

import java.util.ArrayDeque;
import java.util.Queue;

/** Owns mutable queue and relay tuning state for Scala-composed hub hosts. */
public final class HubRuntimeStateJava {
    private final Queue<HubPacketEntry> queue = new ArrayDeque<>();
    private final MovingAverage packetsPerCycleAverage = new MovingAverage(20);
    private int maxQueueSize;
    private int relayDelay;
    private int relayAmount;
    private int relayCooldown = -1;

    public HubRuntimeStateJava(int maxQueueSize, int relayDelay, int relayAmount) {
        this.maxQueueSize = maxQueueSize;
        this.relayDelay = relayDelay;
        this.relayAmount = relayAmount;
    }

    public Queue<HubPacketEntry> queue() { return queue; }
    public int maxQueueSize() { return maxQueueSize; }
    public void setMaxQueueSize(int value) { maxQueueSize = value; }
    public int relayDelay() { return relayDelay; }
    public void setRelayDelay(int value) { relayDelay = value; }
    public int relayAmount() { return relayAmount; }
    public void setRelayAmount(int value) { relayAmount = value; }
    public int relayCooldown() { return relayCooldown; }
    public void setRelayCooldown(int value) { relayCooldown = value; }
    public MovingAverage packetsPerCycleAverage() { return packetsPerCycleAverage; }
}
