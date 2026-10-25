package li.cil.oc.common.container;

import li.cil.oc.api.network.ManagedEnvironment;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ComponentInventoryBehaviorJavaTest {
    @Test
    public void updatesComponentsAddedDuringIteration() {
        List<ManagedEnvironment> updating = new ArrayList<>();
        AtomicInteger updates = new AtomicInteger();
        ManagedEnvironment addedDuringUpdate = environment(updates, null);
        ManagedEnvironment first = environment(updates, () -> updating.add(addedDuringUpdate));
        updating.add(first);
        ComponentInventory inventory = (ComponentInventory) Proxy.newProxyInstance(
                ComponentInventory.class.getClassLoader(),
                new Class<?>[]{ComponentInventory.class},
                (proxy, method, arguments) -> method.getName().equals("updatingComponents") ? updating : null);

        ComponentInventoryBehaviorJava.updateComponents(inventory);

        assertEquals(2, updates.get());
    }

    private static ManagedEnvironment environment(AtomicInteger updates, Runnable onUpdate) {
        return (ManagedEnvironment) Proxy.newProxyInstance(
                ManagedEnvironment.class.getClassLoader(),
                new Class<?>[]{ManagedEnvironment.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("update")) {
                        updates.incrementAndGet();
                        if (onUpdate != null) onUpdate.run();
                    }
                    return null;
                });
    }
}
