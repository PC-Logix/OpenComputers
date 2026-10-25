package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.Assert.*;

public final class HubHostTest {
    @Test
    public void queueTicksAndLegacyAdaptersDispatchThroughNativeHostOverrides() {
        Host host = new Host();
        Packet packet = packet();
        assertTrue(host.tryEnqueuePacketJava(Optional.of(Direction.EAST), packet));
        assertTrue(host.tryEnqueuePacket(scala.Option.apply(Direction.WEST), packet));
        assertEquals(2, host.admissions);
        host.relayCooldown_$eq(0);
        HubLifecycleJava.tick(host, 0);
        assertEquals(List.of(Optional.of(Direction.EAST), Optional.of(Direction.WEST)), host.relayed);
        assertTrue(host.queue().isEmpty());
        host.relayPacket(scala.Option.apply(Direction.NORTH), packet);
        assertEquals(Optional.of(Direction.NORTH), host.relayed.get(2));
    }

    @Test
    public void plugsRetainOwnerCallbacksAndOnlyPrimaryPlugDispatchesMessages() {
        Host host = new Host();
        assertNull(host.sidedNode(null));
        assertFalse(host.canConnect(null));
        Hub.Plug primary = host.plugs()[0];
        primary.onConnect(primary.node());
        primary.onDisconnect(primary.node());
        Message message = (Message) Proxy.newProxyInstance(Message.class.getClassLoader(), new Class<?>[]{Message.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "name" -> "network.message";
                case "source" -> node();
                case "data" -> new Object[]{packet()};
                default -> null;
            });
        primary.onMessage(message);
        host.plugs()[1].onMessage(message);
        assertEquals(1, host.connects);
        assertEquals(1, host.disconnects);
        assertEquals(1, host.admissions);
        assertEquals(Optional.of(Direction.DOWN), host.queue().peek().sourceSideOptional());
    }

    @Test
    public void saveLocksUpstreamEnvironmentAndPlugSnapshotTogether() {
        Host host = new Host();
        CompoundTag tag = new CompoundTag();
        HubLifecycleJava.saveAfterEnvironment(host, tag, null, () -> {
            assertTrue(Thread.holdsLock(host.queue()));
            tag.putBoolean("upstream", true);
        });
        assertTrue(tag.getBoolean("upstream"));
        assertEquals(6, tag.getList("oc:plugs", 10).size());
        assertFalse(Thread.holdsLock(host.queue()));
    }

    private static class Host implements Hub {
        private final HubStateJava state = new HubStateJava(this, 8, 2, 4);
        private int admissions;
        private int connects;
        private int disconnects;
        private final List<Optional<Direction>> relayed = new ArrayList<>();
        @Override public HubStateJava hubState() { return state; }
        @Override public Node createNode(Hub.Plug plug) { return HubHostTest.node(); }
        @Override public Node node() { return null; }
        @Override public Level getLevel() { return null; }
        @Override public boolean isServer() { return true; }
        @Override public void updateEntity() { }
        @Override public void onMessage(Message message) { }
        @Override public void onConnect(Node node) { }
        @Override public void onDisconnect(Node node) { }
        @Override public boolean tryEnqueuePacketJava(Optional<Direction> side, Packet packet) {
            admissions++;
            return Hub.super.tryEnqueuePacketJava(side, packet);
        }
        @Override public void relayPacketJava(Optional<Direction> side, Packet packet) { relayed.add(side); }
        @Override public void onPlugConnect(Plug plug, Node node) { connects++; }
        @Override public void onPlugDisconnect(Plug plug, Node node) { disconnects++; }
    }
    private static Node node() {
        return (Node) Proxy.newProxyInstance(Node.class.getClassLoader(), new Class<?>[]{Node.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                default -> null;
            });
    }
    private static Packet packet() {
        return (Packet) Proxy.newProxyInstance(Packet.class.getClassLoader(), new Class<?>[]{Packet.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "ttl" -> 4;
                case "hop" -> proxy;
                default -> null;
            });
    }
}
