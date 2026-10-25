package li.cil.oc.common.container;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.util.Lifecycle;
import net.minecraft.world.item.ItemStack;
import scala.Option;
import scala.Option$;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Java-owned component-inventory lifecycle and slot operations. */
public final class ComponentInventoryBehaviorJava {
    private ComponentInventoryBehaviorJava() {}

    @SuppressWarnings("unchecked")
    public static Option<ManagedEnvironment>[] componentSlots(ComponentInventory inventory) {
        Option<ManagedEnvironment>[] components = inventory.componentInventoryState().components();
        if (components == null && inventory.isSizeInventoryReady()) {
            components = (Option<ManagedEnvironment>[]) new Option<?>[inventory.getContainerSize()];
            Arrays.fill(components, Option$.MODULE$.empty());
            inventory.componentInventoryState().components(components);
        }
        return components == null ? (Option<ManagedEnvironment>[]) new Option<?>[0] : components;
    }

    public static void updateComponents(ComponentInventory inventory) {
        List<ManagedEnvironment> updating = inventory.updatingComponents();
        int index = 0;
        // The list may change during update (for example, a component can remove
        // itself). Re-read its size each iteration rather than iterating a snapshot.
        while (index < updating.size()) {
            updating.get(index).update();
            index++;
        }
    }

    public static void connectComponents(ComponentInventory inventory) {
        EnvironmentHost host = inventory.host();
        // Block entities may deserialize components before Minecraft attaches
        // their Level. Defer driver construction until the host is live.
        if (host.getEnvironmentLevel() == null) return;

        int slotCount = Math.min(inventory.getContainerSize(), ComponentInventorySlotAccessJava.size(inventory));
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || ComponentInventorySlotAccessJava.get(inventory, slot).isPresent() || !inventory.isComponentSlot(slot, stack)) continue;

            DriverItem driver = Driver.driverFor(stack);
            if (driver == null) continue;
            ManagedEnvironment component = driver.createEnvironment(stack, host);
            if (component == null) continue;

            inventory.applyLifecycleState(component, Lifecycle.LifecycleState.Constructing);
            try {
                inventory.load(component, driver, stack);
            } catch (Throwable error) {
                OpenComputers.log().warn("An item component of type '{}' (provided by driver '{}') threw an error while loading.",
                        component.getClass().getName(), driver.getClass().getName(), error);
            }
            if (component.canUpdate()) {
                List<ManagedEnvironment> updating = inventory.updatingComponents();
                assert !updating.contains(component);
                updating.add(component);
            }
            ComponentInventorySlotAccessJava.set(inventory, slot, component);
        }

        Network.joinNewNetwork(inventory.node());
        for (int slot = 0; slot < slotCount; slot++) {
            Optional<ManagedEnvironment> value = ComponentInventorySlotAccessJava.get(inventory, slot);
            if (value.isEmpty()) continue;
            ManagedEnvironment component = value.get();
            inventory.applyLifecycleState(component, Lifecycle.LifecycleState.Initializing);
            inventory.connectItemNode(component.node());
            inventory.applyLifecycleState(component, Lifecycle.LifecycleState.Initialized);
        }
    }

    public static void disconnectComponents(ComponentInventory inventory) {
        for (Optional<ManagedEnvironment> slot : ComponentInventorySlotAccessJava.snapshot(inventory)) {
            if (slot.isEmpty()) continue;
            ManagedEnvironment component = slot.get();
            inventory.applyLifecycleState(component, Lifecycle.LifecycleState.Disposing);
            if (component.node() != null) component.node().remove();
            inventory.applyLifecycleState(component, Lifecycle.LifecycleState.Disposed);
        }
    }

    public static void saveComponents(ComponentInventory inventory) {
        int slotCount = Math.min(inventory.getContainerSize(), ComponentInventorySlotAccessJava.size(inventory));
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            if (slot >= slotCount) {
                OpenComputers.log().error("ComponentInventory components length {} does not accommodate inventory size {}",
                        ComponentInventorySlotAccessJava.size(inventory), inventory.getContainerSize());
                return;
            }
            Optional<ManagedEnvironment> component = ComponentInventorySlotAccessJava.get(inventory, slot);
            if (component.isPresent()) {
                DriverItem driver = Driver.driverFor(stack);
                if (driver != null) inventory.save(component.get(), driver, stack);
            }
        }
    }

    public static void onItemAdded(ComponentInventory inventory, int slot, ItemStack stack) {
        if (slot < 0 || slot >= ComponentInventorySlotAccessJava.size(inventory) || !inventory.isComponentSlot(slot, stack)) return;
        DriverItem driver = Driver.driverFor(stack);
        if (driver == null) return;
        ManagedEnvironment component = driver.createEnvironment(stack, inventory.host());
        if (component == null) return;

        synchronized (inventory) {
            ComponentInventorySlotAccessJava.set(inventory, slot, component);
            inventory.applyLifecycleState(component, Lifecycle.LifecycleState.Constructing);
            try {
                inventory.load(component, driver, stack);
            } catch (Throwable error) {
                OpenComputers.log().warn("An item component of type '{}' (provided by driver '{}') threw an error while loading.",
                        component.getClass().getName(), driver.getClass().getName(), error);
            }
            if (component.canUpdate()) {
                List<ManagedEnvironment> updating = inventory.updatingComponents();
                assert !updating.contains(component);
                updating.add(component);
            }
            inventory.applyLifecycleState(component, Lifecycle.LifecycleState.Initializing);
            inventory.connectItemNode(component.node());
            inventory.applyLifecycleState(component, Lifecycle.LifecycleState.Initialized);
            inventory.save(component, driver, stack);
        }
    }

    public static void onItemRemoved(ComponentInventory inventory, int slot, ItemStack stack) {
        Optional<ManagedEnvironment> current = ComponentInventorySlotAccessJava.get(inventory, slot);
        if (current.isEmpty()) return;

        synchronized (inventory) {
            ManagedEnvironment component = current.get();
            // Remove the node before saving so handles close before persistence,
            // then remove it again after save to clear addresses rejoined by the environment.
            ComponentInventorySlotAccessJava.set(inventory, slot, null);
            inventory.updatingComponents().remove(component);
            inventory.applyLifecycleState(component, Lifecycle.LifecycleState.Disposing);
            if (component.node() != null) component.node().remove();
            DriverItem driver = Driver.driverFor(stack);
            if (driver != null) inventory.save(component, driver, stack);
            if (component.node() != null) component.node().remove();
            inventory.applyLifecycleState(component, Lifecycle.LifecycleState.Disposed);
        }
    }

    public static void connectItemNode(ComponentInventory inventory, Node node) {
        Node hostNode = inventory.node();
        if (hostNode != null && node != null) hostNode.connect(node);
    }
}
