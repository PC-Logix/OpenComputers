package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.junit.Test;
import scala.Option;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AbstractComponentInventoryTest {
    @Test
    public void javaHostInheritsInventoryStateAndLazySlotInitialization() {
        TestInventory inventory = new TestInventory();

        Option<?>[] first = inventory.componentSlots();

        assertEquals(2, first.length);
        assertTrue(first[0].isEmpty());
        assertSame(first, inventory.componentSlots());
        assertTrue(inventory.updatingComponents().isEmpty());

        TestInventory loading = new TestInventory();
        loading.setInventorySizeReady(false);
        assertEquals(0, loading.componentSlots().length);
        loading.setInventorySizeReady(true);
        assertEquals(2, loading.componentSlots().length);
        assertFalse(loading.componentSlots()[1].isDefined());
    }

    @Test
    public void javaOptionalFacadeWritesThroughToScalaInventoryState() {
        TestInventory inventory = new TestInventory();
        ManagedEnvironment component = (ManagedEnvironment) Proxy.newProxyInstance(
                ManagedEnvironment.class.getClassLoader(),
                new Class<?>[]{ManagedEnvironment.class},
                (proxy, method, arguments) -> null);

        ComponentInventorySlotAccessJava.set(inventory, 1, component);
        Optional<ManagedEnvironment>[] slots = ComponentInventorySlotAccessJava.snapshot(inventory);
        assertTrue(slots[1].isPresent());
        assertSame(component, slots[1].get());
        assertTrue(inventory.componentSlots()[1].isDefined());

        ComponentInventorySlotAccessJava.set(inventory, 1, null);
        assertFalse(ComponentInventorySlotAccessJava.snapshot(inventory)[1].isPresent());
        assertTrue(inventory.componentSlots()[1].isEmpty());
    }

    private static final class TestInventory extends AbstractComponentInventory {
        @Override public EnvironmentHost host() { return null; }
        @Override public ItemStack[] items() { return new ItemStack[2]; }
        @Override public int getContainerSize() { return 2; }
        @Override public void setChanged() {}
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return true; }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public Node node() { return null; }
        @Override public void onConnect(Node node) {}
        @Override public void onDisconnect(Node node) {}
        @Override public void onMessage(Message message) {}
    }
}
