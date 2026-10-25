package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Node;
import net.minecraft.core.Direction;
import org.junit.Test;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public final class ComputerHostTest {
    @Test
    public void nativeHostTicksMachineThenObservesStateThenUpdatesComponents() {
        HostState state = new HostState();
        Computer host = state.host();
        ComputerLifecycleJava.beforeUpdate(host);
        assertEquals(List.of("machine", "running", "components"), state.events);
        assertTrue(host.isRunning());
        assertFalse(host.hasErrored());
        state.events.clear();
        ComputerLifecycleJava.beforeUpdate(host);
        assertEquals(List.of("machine", "components"), state.events);
        state.events.clear();
        state.connected = false;
        ComputerLifecycleJava.beforeUpdate(host);
        assertTrue(state.events.isEmpty());
    }

    @Test
    public void nativeMachineAndRedstoneCallbacksDispatchThroughHostAndUseLocalSide() {
        HostState state = new HostState();
        Computer host = state.host();
        host.onMachineConnect(state.node);
        host.onMachineDisconnect(state.node);
        host.setInput(Direction.NORTH, 2);
        host.setInput(Direction.NORTH, 6);
        assertEquals(List.of("connect", "disconnect", "redstone"), state.events);
        assertEquals(host.toLocal(Direction.NORTH), state.redstoneChange.side());
        assertEquals(2, state.redstoneChange.oldValue());
        assertEquals(6, state.redstoneChange.newValue());
        assertTrue(host.redstoneAfterInventory());
        assertFalse(new HostState().host().isRunning());
    }

    private static final class HostState {
        final ComputerStateJava state = new ComputerStateJava();
        final RedstoneStateJava redstone = new RedstoneStateJava();
        final List<String> events = new ArrayList<>();
        boolean connected = true;
        RedstoneChangedEventArgs redstoneChange;
        final Node node = (Node) Proxy.newProxyInstance(Node.class.getClassLoader(), new Class<?>[]{Node.class},
            (proxy, method, args) -> {
                if (method.getName().equals("sendToNeighbors")) {
                    events.add("redstone");
                    redstoneChange = (RedstoneChangedEventArgs) ((Object[]) args[1])[0];
                }
                return null;
            });
        Computer host() {
            Machine machine = (Machine) Proxy.newProxyInstance(Machine.class.getClassLoader(), new Class<?>[]{Machine.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "node" -> node;
                    case "isRunning" -> true;
                    case "lastError" -> null;
                    case "update" -> { events.add("machine"); yield null; }
                    default -> null;
                });
            state.machine.get(true, () -> machine);
            return (Computer) Proxy.newProxyInstance(Computer.class.getClassLoader(), new Class<?>[]{Computer.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "computerState" -> state;
                    case "redstoneState" -> redstone;
                    case "isServer" -> true;
                    case "computerNetworkConnected" -> connected;
                    case "onRunningChanged" -> { events.add("running"); yield null; }
                    case "updateComponents" -> { events.add("components"); yield null; }
                    case "onConnect" -> { events.add("connect"); yield null; }
                    case "onDisconnect" -> { events.add("disconnect"); yield null; }
                    case "pitch" -> Direction.NORTH;
                    case "yaw" -> Direction.EAST;
                    default -> {
                        if (method.isDefault()) yield InvocationHandler.invokeDefault(proxy, method, args);
                        throw new AssertionError("Unexpected host callback: " + method);
                    }
                });
        }
    }
}
