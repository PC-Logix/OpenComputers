package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.container.ComponentInventoryBehaviorJava;
import net.minecraft.world.item.ItemStack;

/** Component-inventory behavior for block entities, using standard Java host hooks. */
public interface ComponentInventory extends li.cil.oc.common.container.ComponentInventory, EnvironmentHost {
    ComponentInventoryHostStateJava componentHostState();
    boolean isServer();
    boolean isClient();
    void setChanged();
    @Override default State componentInventoryState() { return componentHostState().inventory; }
    @Override default EnvironmentHost host() { return this; }
    default void scheduleInventoryChanges(Runnable action) { li.cil.oc.common.EventHandler.scheduleClientTask(action); }
    @Override default void onItemAdded(int slot, ItemStack stack) {
        if (isServer()) ComponentInventoryBehaviorJava.onItemAdded(this, slot, stack);
        else componentHostState().clientChanges.onItemAdded(slot, this::getContainerSize, stack, ItemStack::matches, ItemStack::isEmpty,
            (index, removed) -> ComponentInventoryBehaviorJava.onItemRemoved(this, index, removed),
            (index, added) -> ComponentInventoryBehaviorJava.onItemAdded(this, index, added), this::setChanged, this::scheduleInventoryChanges);
    }
    @Override default void onItemRemoved(int slot, ItemStack stack) {
        if (isServer()) ComponentInventoryBehaviorJava.onItemRemoved(this, slot, stack);
        else componentHostState().clientChanges.onItemRemoved(slot, this::getContainerSize, stack, ItemStack::matches, ItemStack::isEmpty,
            (index, removed) -> ComponentInventoryBehaviorJava.onItemRemoved(this, index, removed),
            (index, added) -> ComponentInventoryBehaviorJava.onItemAdded(this, index, added), this::setChanged, this::scheduleInventoryChanges);
    }
    @Override default void save(ManagedEnvironment component, DriverItem driver, ItemStack stack) {
        if (isServer()) li.cil.oc.common.container.ComponentInventoryPersistenceJava.save(component, driver, stack);
    }
}
