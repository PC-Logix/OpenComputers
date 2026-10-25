package li.cil.oc.common.container;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.Lifecycle;
import li.cil.oc.integration.opencomputers.DriverTablet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.function.Consumer;

/** Java persistence boundary for environments installed in component inventories. */
public final class ComponentInventoryPersistenceJava {
    private ComponentInventoryPersistenceJava() {}

    public static void load(ManagedEnvironment component, DriverItem driver, ItemStack stack) {
        Consumer<ItemStack> loadFrom = persistenceStack -> {
            try {
                if (component.node() != null) component.node().loadData(persistenceStack);
                component.loadData(persistenceStack);
            } catch (UnrecoverablePersistanceException error) {
                ComponentInventoryPersistenceJava.<RuntimeException>throwUnchecked(error);
            }
        };

        if (driver == DriverTablet.INSTANCE) {
            // A tablet exposes its embedded filesystem as the component. Loading
            // the outer stack would consume the tablet's own battery components.
            DriverTablet.withFileSystemStack(stack, loadFrom);
            // Tablet drives are normally neighbor-visible. Restore this override
            // for charger slots after loading the persisted node state.
            if (component.node() instanceof Component node) {
                node.setVisibility(Visibility.Network);
            }
        } else {
            loadFrom.accept(stack);
        }
    }

    public static void save(ManagedEnvironment component, DriverItem driver, ItemStack stack) {
        try {
            Consumer<ItemStack> saveTo = persistenceStack -> {
                if (driver instanceof li.cil.oc.api.driver.item.Memory) {
                    // RAM has no persistent per-item state; old builds could
                    // leave temporary node identity on otherwise stackable RAM.
                    clearMemoryIdentity(persistenceStack);
                    return;
                }

                component.saveData(persistenceStack);

                // Preserve node identity even when an environment forgets to
                // call super.saveData().
                if (component.node() != null) {
                    component.node().saveData(persistenceStack);
                    String persisted = persistenceStack.get(OCComponentsJava.ADDRESS);
                    if (component.node().address() != null && !component.node().address().equals(persisted)) {
                        OpenComputers.log().error("Failed to persist component node address for {}: node={}, stack={}",
                                component.getClass().getName(), component.node().address(), persisted);
                    }
                }
            };

            if (driver == DriverTablet.INSTANCE) {
                // Persist the drive environment only into the tablet's nested
                // filesystem stack, never onto the tablet's battery state.
                DriverTablet.withFileSystemStack(stack, saveTo);
            } else {
                saveTo.accept(stack);
            }
        } catch (Throwable error) {
            OpenComputers.log().warn("An item component of type '{}' (provided by driver '{}') threw an error while saving.",
                    component.getClass().getName(), driver.getClass().getName(), error);
        }
    }

    public static void applyLifecycleState(Object component, Lifecycle.LifecycleState state) {
        if (component instanceof Lifecycle lifecycle) lifecycle.onLifecycleStateChange(state);
    }

    public static void clearMemoryIdentity(MutableDataComponentHolder holder) {
        clearMemoryIdentity(holder,
                OCComponentsJava.ADDRESS.get(),
                OCComponentsJava.VISIBILITY.get());
    }

    static void clearMemoryIdentity(MutableDataComponentHolder holder,
                                    DataComponentType<?> address,
                                    DataComponentType<?> visibility) {
        holder.remove(address);
        holder.remove(visibility);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable error) throws T {
        throw (T) error;
    }
}
