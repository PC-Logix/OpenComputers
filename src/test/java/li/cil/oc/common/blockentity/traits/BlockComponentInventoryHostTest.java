package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public final class BlockComponentInventoryHostTest {
    @Test
    public void javaHostKeepsClientSnapshotAndConnectionLifecycleOrder() {
        List<String> events = new ArrayList<>();
        ComponentInventoryHostStateJava state = new ComponentInventoryHostStateJava();
        Node node = (Node) Proxy.newProxyInstance(Node.class.getClassLoader(), new Class<?>[]{Node.class}, (proxy, method, args) -> null);
        ComponentInventory host = (ComponentInventory) Proxy.newProxyInstance(ComponentInventory.class.getClassLoader(), new Class<?>[]{ComponentInventory.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "componentHostState" -> state;
                case "isClient" -> true;
                case "node" -> node;
                case "connectComponents" -> { events.add("connect"); yield null; }
                case "disconnectComponents" -> { events.add("disconnect"); yield null; }
                case "loadData" -> { events.add("load"); yield null; }
                case "saveData" -> { events.add("save"); yield null; }
                default -> {
                    if (method.isDefault()) yield InvocationHandler.invokeDefault(proxy, method, args);
                    throw new AssertionError("Unexpected callback " + method);
                }
            });
        assertSame(host, host.host());
        assertSame(state.inventory, host.componentInventoryState());
        ComponentInventoryLifecycleJava.initialize(host);
        ComponentInventoryLifecycleJava.dispose(host);
        ComponentInventoryLifecycleJava.onConnect(host, null);
        ComponentInventoryLifecycleJava.onConnect(host, node);
        ComponentInventoryLifecycleJava.onDisconnect(host, node);
        assertEquals(List.of("connect", "disconnect", "connect", "disconnect"), events);
        events.clear();
        ComponentInventoryLifecycleJava.beforeSaveClient(host);
        events.add("base");
        ComponentInventoryLifecycleJava.afterSaveClient(host, new CompoundTag(), null);
        assertEquals(List.of("connect", "base", "save"), events);
        events.clear();
        ComponentInventoryLifecycleJava.afterLoadClient(host, new CompoundTag(), null);
        assertEquals(List.of("load", "connect"), events);
    }
}
