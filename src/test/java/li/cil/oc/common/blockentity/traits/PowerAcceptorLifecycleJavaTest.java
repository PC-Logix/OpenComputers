package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Connector;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public final class PowerAcceptorLifecycleJavaTest {
    @Test
    public void preservesPowerBeforeEnvironmentOrder() {
        assertEquals(List.of("power-initialize", "environment-initialize", "power-update", "environment-update",
            "power-load", "environment-load", "power-save", "environment-save", "power-dispose"), runPhases(false));
    }

    @Test
    public void preservesPowerAfterEnvironmentOrder() {
        assertEquals(List.of("environment-initialize", "power-initialize", "environment-update", "power-update",
            "environment-load", "power-load", "environment-save", "power-save", "power-dispose"), runPhases(true));
    }

    @Test
    public void relayPowerCallbacksWaitForHubTickAndPersistence() {
        assertEquals(List.of("environment-initialize", "power-initialize",
            "environment-update", "hub-update", "power-update",
            "environment-load", "hub-load", "power-load",
            "environment-save", "hub-save", "power-save", "power-dispose"), runPhases(true, true));
    }
    @Test
    public void javaConnectorContractRetainsLegacyAccessAndClientShortCircuit() {
        Connector connector = (Connector) Proxy.newProxyInstance(Connector.class.getClassLoader(),
            new Class<?>[]{Connector.class}, (proxy, method, args) -> switch (method.getName()) {
                case "globalBuffer" -> 25.0;
                case "globalBufferSize" -> 100.0;
                default -> null;
            });
        TestHost server = new TestHost(false) {
            @Override public Optional<Connector> connectorOptional(Direction side) {
                return side == Direction.NORTH ? Optional.of(connector) : Optional.empty();
            }
        };
        assertSame(connector, server.connector(Direction.NORTH).get());
        assertEquals(25.0, server.globalBuffer(Direction.NORTH), 0);
        assertEquals(10.0, server.globalDemand(Direction.NORTH), 0);
        assertEquals(0.0, server.globalBuffer(Direction.SOUTH), 0);

        TestHost client = new TestHost(false) {
            @Override public boolean isClient() { return true; }
            @Override public Optional<Connector> connectorOptional(Direction side) {
                throw new AssertionError("client power queries must not resolve server connectors");
            }
        };
        assertEquals(0.0, client.globalBuffer(Direction.NORTH), 0);
        assertEquals(0.0, client.globalBufferSize(Direction.NORTH), 0);
    }

    private static List<String> runPhases(boolean afterEnvironment) {
        return runPhases(afterEnvironment, false);
    }

    private static List<String> runPhases(boolean afterEnvironment, boolean afterHub) {
        List<String> events = new ArrayList<>();
        PowerAcceptorIntegration previous = PowerAcceptorHooks.integration();
        try {
            PowerAcceptorHooks.install(new PowerAcceptorIntegration() {
                @Override public void initialize(PowerAcceptor host) { events.add("power-initialize"); }
                @Override public void update(PowerAcceptor host) { events.add("power-update"); }
                @Override public void dispose(PowerAcceptor host) { events.add("power-dispose"); }
                @Override public void load(PowerAcceptor host, CompoundTag nbt, HolderLookup.Provider provider) {
                    events.add("power-load");
                }
                @Override public void save(PowerAcceptor host, CompoundTag nbt, HolderLookup.Provider provider) {
                    events.add("power-save");
                }
            });
            TestHost host = new TestHost(afterEnvironment, afterHub);
            PowerAcceptorLifecycleJava.initialize(host, false);
            events.add("environment-initialize");
            PowerAcceptorLifecycleJava.initialize(host, true);
            PowerAcceptorLifecycleJava.update(host, false);
            events.add("environment-update");
            PowerAcceptorLifecycleJava.update(host, true);
            if (afterHub) events.add("hub-update");
            PowerAcceptorLifecycleJava.updateAfterHub(host);
            PowerAcceptorLifecycleJava.load(host, false, null, null);
            events.add("environment-load");
            PowerAcceptorLifecycleJava.load(host, true, null, null);
            if (afterHub) events.add("hub-load");
            PowerAcceptorLifecycleJava.loadAfterHub(host, null, null);
            PowerAcceptorLifecycleJava.save(host, false, null, null);
            events.add("environment-save");
            PowerAcceptorLifecycleJava.save(host, true, null, null);
            if (afterHub) events.add("hub-save");
            PowerAcceptorLifecycleJava.saveAfterHub(host, null, null);
            PowerAcceptorLifecycleJava.dispose(host);
            PowerAcceptorLifecycleJava.initialize(new Object(), false);
            PowerAcceptorLifecycleJava.dispose(new Object());
            return events;
        } finally {
            PowerAcceptorHooks.install(previous);
        }
    }

    private static class TestHost implements PowerAcceptor {
        private final boolean after;
        private final boolean afterHub;
        private TestHost(boolean after) { this(after, false); }
        private TestHost(boolean after, boolean afterHub) { this.after = after; this.afterHub = afterHub; }
        @Override public boolean isClient() { return false; }
        @Override public boolean isServer() { return !isClient(); }
        @Override public double energyThroughput() { return 10; }
        @Override public boolean powerLifecycleAfterEnvironment() { return after; }
        @Override public boolean powerLifecycleAfterHub() { return afterHub; }
    }
}
