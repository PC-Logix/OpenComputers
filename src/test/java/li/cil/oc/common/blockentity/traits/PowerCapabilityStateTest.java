package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public final class PowerCapabilityStateTest {
    @Test
    public void nativeHostPowerSnapshotRoundTripsExistingClientTags() {
        NativeBalancer host = new NativeBalancer();
        host.globalBuffer_$eq(25);
        host.globalBufferSize_$eq(100);
        CompoundTag snapshot = new CompoundTag();
        PowerInformationJava.saveHostForClient(host, snapshot);
        assertEquals(25, snapshot.getDouble("oc:globalBuffer"), 0);
        assertEquals(100, snapshot.getDouble("oc:globalBufferSize"), 0);
        assertFalse(host.powerInformationState().hasChangedSignificantly(0.25));

        host.globalBuffer_$eq(0);
        host.globalBufferSize_$eq(0);
        PowerInformationJava.loadHostForClient(host, snapshot);
        assertEquals(25, host.globalBuffer(), 0);
        assertEquals(100, host.globalBufferSize(), 0);
        assertEquals(0, new NativeBalancer().globalBuffer(), 0);
    }

    @Test
    public void nativeBalancerCountsEachNetworkOnlyOnce() {
        NativeBalancer host = new NativeBalancer();
        Network shared = network();
        host.sides[Direction.DOWN.ordinal()] = connector(shared, 25, 100);
        host.sides[Direction.UP.ordinal()] = connector(shared, 75, 100);
        host.sides[Direction.NORTH.ordinal()] = connector(network(), 10, 50);

        assertArrayEquals(new double[]{35, 150}, host.distributeJava(), 0);
    }

    private static Network network() {
        return (Network) Proxy.newProxyInstance(Network.class.getClassLoader(), new Class<?>[]{Network.class},
            (proxy, method, args) -> method.getName().equals("equals") ? proxy == args[0] : null);
    }

    private static Connector connector(Network network, double buffer, double size) {
        return (Connector) Proxy.newProxyInstance(Connector.class.getClassLoader(), new Class<?>[]{Connector.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "network" -> network;
                case "globalBuffer" -> buffer;
                case "globalBufferSize" -> size;
                default -> null;
            });
    }

    private static final class NativeBalancer implements PowerBalancer {
        private final PowerBalancerStateJava state = new PowerBalancerStateJava();
        private final Node[] sides = new Node[Direction.values().length];

        @Override public PowerBalancerStateJava powerBalancerState() { return state; }
        @Override public boolean isServer() { return true; }
        @Override public boolean powerNetworkConnected() { return true; }
        @Override public Node sidedNode(Direction side) { return sides[side.ordinal()]; }
        @Override public boolean canConnect(Direction side) { return sidedNode(side) != null; }
        @Override public void updateEntity() {}
    }
}
