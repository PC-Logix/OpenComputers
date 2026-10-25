package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.*;
import li.cil.oc.util.MovingAverage;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

/** Sided network hub with host-owned Java state and overridable plug callbacks. */
public interface Hub extends li.cil.oc.api.network.Environment, SidedEnvironment, Tickable {
    HubStateJava hubState();
    Level getLevel();
    boolean isServer();

    default Plug[] plugs() { return hubState().plugs(); }
    default Node[] plugNodes() { return Arrays.stream(plugs()).map(plug -> plug == null ? null : plug.node()).toArray(Node[]::new); }
    default Queue<HubPacketEntry> queue() { return hubState().runtime().queue(); }
    default int maxQueueSize() { return hubState().runtime().maxQueueSize(); }
    default void maxQueueSize_$eq(int value) { hubState().runtime().setMaxQueueSize(value); }
    default int relayDelay() { return hubState().runtime().relayDelay(); }
    default void relayDelay_$eq(int value) { hubState().runtime().setRelayDelay(value); }
    default int relayAmount() { return hubState().runtime().relayAmount(); }
    default void relayAmount_$eq(int value) { hubState().runtime().setRelayAmount(value); }
    default int relayCooldown() { return hubState().runtime().relayCooldown(); }
    default void relayCooldown_$eq(int value) { hubState().runtime().setRelayCooldown(value); }
    default MovingAverage packetsPerCycleAvg() { return hubState().runtime().packetsPerCycleAverage(); }
    @Override default boolean canConnect(Direction side) { return side != null; }
    @Override default Node sidedNode(Direction side) { return side != null ? plugs()[side.ordinal()].node() : null; }
    default boolean tryEnqueuePacket(Option<Direction> sourceSide, Packet packet) {
        return tryEnqueuePacketJava(OptionConverters.toJava(sourceSide), packet);
    }
    default boolean tryEnqueuePacketJava(Optional<Direction> sourceSide, Packet packet) {
        var result = HubPacketQueueJava.tryEnqueue(queue(), sourceSide, packet, maxQueueSize(), relayCooldown(), relayDelay());
        relayCooldown_$eq(result.relayCooldown());
        return result.enqueued();
    }
    default void relayPacket(Option<Direction> sourceSide, Packet packet) { relayPacketJava(OptionConverters.toJava(sourceSide), packet); }
    default void relayPacketJava(Optional<Direction> sourceSide, Packet packet) { HubNetworkJava.relayPacket(this, sourceSide, packet); }
    default Plug createPlug(Direction side) { return new Plug(this, side); }
    default Node createNode(Plug plug) { return li.cil.oc.api.Network.newNode(plug, Visibility.Network).create(); }
    default void onPlugConnect(Plug plug, Node node) { }
    default void onPlugDisconnect(Plug plug, Node node) { }
    default void onPlugMessage(Plug plug, Message message) {
        HubNetworkJava.onPlugMessageJava(plugNodes(), plug.side(), message, this::tryEnqueuePacketJava);
    }

    class Plug implements li.cil.oc.api.network.Environment {
        private final Hub host;
        private final Direction side;
        private final Node node;
        public Plug(Hub host, Direction side) {
            this.host = host;
            this.side = side;
            this.node = host.createNode(this);
        }
        public Direction side() { return side; }
        @Override public Node node() { return node; }
        @Override public void onMessage(Message message) { if (isPrimary()) host.onPlugMessage(this, message); }
        @Override public void onConnect(Node node) { host.onPlugConnect(this, node); }
        @Override public void onDisconnect(Node node) { host.onPlugDisconnect(this, node); }
        public boolean isPrimary() { return HubNetworkJava.isPrimary(node, host.plugNodes()); }
        public Plug[] plugsInOtherNetworks() {
            return Arrays.stream(host.plugs()).filter(plug -> !Objects.equals(plug.node().network(), node.network())).toArray(Plug[]::new);
        }
    }
}


