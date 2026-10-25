package li.cil.oc.common.container;

import li.cil.oc.api.API;
import li.cil.oc.api.detail.DriverAPI;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.util.Lifecycle;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public final class ComponentInventoryHostHooksTest {
    @Test
    public void installingAndRemovingAComponentUsesHostRoutingAndPersistenceHooks() {
        List<String> events = new ArrayList<>();
        HookInventory inventory = new HookInventory(events);
        Node node = (Node) Proxy.newProxyInstance(Node.class.getClassLoader(), new Class<?>[]{Node.class},
            (proxy, method, arguments) -> {
                if (method.getName().equals("remove")) events.add("remove-node");
                return null;
            });
        ManagedEnvironment component = (ManagedEnvironment) Proxy.newProxyInstance(
            ManagedEnvironment.class.getClassLoader(), new Class<?>[]{ManagedEnvironment.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "node" -> node;
                case "canUpdate" -> false;
                default -> null;
            });
        DriverItem driver = (DriverItem) Proxy.newProxyInstance(DriverItem.class.getClassLoader(),
            new Class<?>[]{DriverItem.class},
            (proxy, method, arguments) -> method.getName().equals("createEnvironment") ? component : null);

        DriverAPI previous = API.driver;
        try {
            API.driver = (DriverAPI) Proxy.newProxyInstance(DriverAPI.class.getClassLoader(),
                new Class<?>[]{DriverAPI.class},
                (proxy, method, arguments) -> method.getName().equals("driverFor") ? driver : null);
            inventory.onItemAdded(0, null);
            assertSame(component, ComponentInventorySlotAccessJava.get(inventory, 0).orElseThrow());
            assertEquals(List.of("Constructing", "load", "Initializing", "route-node", "Initialized", "save"), events);

            events.clear();
            inventory.onItemRemoved(0, null);
            assertTrue(ComponentInventorySlotAccessJava.get(inventory, 0).isEmpty());
            assertEquals(List.of("Disposing", "remove-node", "save", "remove-node", "Disposed"), events);
        } finally {
            API.driver = previous;
        }
    }

    private static final class HookInventory extends AbstractComponentInventory {
        private final List<String> events;
        private final ItemStack[] items = new ItemStack[1];

        private HookInventory(List<String> events) { this.events = events; }
        @Override public EnvironmentHost host() { return null; }
        @Override public ItemStack[] items() { return items; }
        @Override public int getContainerSize() { return items.length; }
        @Override public void setChanged() {}
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return true; }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public Node node() { return null; }
        @Override public void onConnect(Node node) {}
        @Override public void onDisconnect(Node node) {}
        @Override public void onMessage(Message message) {}
        @Override public void connectItemNode(Node node) { events.add("route-node"); }
        @Override public void load(ManagedEnvironment component, DriverItem driver, ItemStack stack) { events.add("load"); }
        @Override public void save(ManagedEnvironment component, DriverItem driver, ItemStack stack) { events.add("save"); }
        @Override public void applyLifecycleState(Object component, Lifecycle.LifecycleState state) { events.add(state.name()); }
    }
}
