package li.cil.oc.common.blockentity.traits;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import li.cil.oc.api.network.Node;
import net.minecraft.core.Direction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public final class ComputerRedstoneJavaTest {
    @Test
    public void sendsLocalRedstoneChangeToComputerNeighbors() {
        AtomicReference<String> eventName = new AtomicReference<>();
        AtomicReference<Object[]> payload = new AtomicReference<>();
        Node node = (Node) Proxy.newProxyInstance(Node.class.getClassLoader(), new Class<?>[]{Node.class},
            (proxy, method, args) -> {
                if (method.getName().equals("sendToNeighbors")) {
                    eventName.set((String) args[0]);
                    payload.set((Object[]) args[1]);
                }
                return null;
            });
        Computer computer = (Computer) Proxy.newProxyInstance(Computer.class.getClassLoader(),
            new Class<?>[]{Computer.class}, (proxy, method, args) ->
                method.getName().equals("node") ? node : null);

        ComputerRedstoneJava.sendInputChanged(computer, Direction.WEST, 1, 12, 3);

        assertEquals("redstone.changed", eventName.get());
        assertEquals(1, payload.get().length);
        RedstoneChangedEventArgs event = (RedstoneChangedEventArgs) payload.get()[0];
        assertSame(Direction.WEST, event.side());
        assertEquals(1, event.oldValue());
        assertEquals(12, event.newValue());
        assertEquals(3, event.color());
    }
}
