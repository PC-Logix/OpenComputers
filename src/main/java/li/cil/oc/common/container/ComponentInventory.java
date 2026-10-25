package li.cil.oc.common.container;

import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.util.Lifecycle;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;

import java.util.ArrayList;
import java.util.List;

/** Component inventory behavior with explicit Java-owned host state. */
public interface ComponentInventory extends Inventory, Environment {
    State componentInventoryState();
    EnvironmentHost host();

    default Option<ManagedEnvironment>[] componentSlots() { return ComponentInventoryBehaviorJava.componentSlots(this); }
    default boolean isSizeInventoryReady() { return componentInventoryState().sizeInventoryReady; }
    default void isSizeInventoryReady_$eq(boolean value) { componentInventoryState().sizeInventoryReady = value; }
    default List<ManagedEnvironment> updatingComponents() { return componentInventoryState().updating; }
    default void updateComponents() { ComponentInventoryBehaviorJava.updateComponents(this); }
    default void connectComponents() { ComponentInventoryBehaviorJava.connectComponents(this); }
    default void disconnectComponents() { ComponentInventoryBehaviorJava.disconnectComponents(this); }

    @Override default DataComponentType<scala.collection.immutable.List<ImmutableItemStack>> component() {
        return OCComponentsJava.COMPONENTS.get();
    }
    @Override default void saveData(MutableDataComponentHolder holder) {
        saveComponents();
        Inventory.saveInventoryData(this, holder);
    }
    default void saveComponents() { ComponentInventoryBehaviorJava.saveComponents(this); }
    @Override default int getMaxStackSize() { return 1; }
    @Override default void onItemAdded(int slot, ItemStack stack) { ComponentInventoryBehaviorJava.onItemAdded(this, slot, stack); }
    @Override default void onItemRemoved(int slot, ItemStack stack) { ComponentInventoryBehaviorJava.onItemRemoved(this, slot, stack); }
    default boolean isComponentSlot(int slot, ItemStack stack) { return true; }
    default void connectItemNode(Node node) { ComponentInventoryBehaviorJava.connectItemNode(this, node); }
    default void load(ManagedEnvironment component, DriverItem driver, ItemStack stack) {
        ComponentInventoryPersistenceJava.load(component, driver, stack);
    }
    default void save(ManagedEnvironment component, DriverItem driver, ItemStack stack) {
        ComponentInventoryPersistenceJava.save(component, driver, stack);
    }
    default void applyLifecycleState(Object component, Lifecycle.LifecycleState state) {
        ComponentInventoryPersistenceJava.applyLifecycleState(component, state);
    }

    final class State {
        private Option<ManagedEnvironment>[] components;
        private boolean sizeInventoryReady = true;
        private final List<ManagedEnvironment> updating = new ArrayList<>();

        Option<ManagedEnvironment>[] components() { return components; }
        void components(Option<ManagedEnvironment>[] value) { components = value; }
    }
}
