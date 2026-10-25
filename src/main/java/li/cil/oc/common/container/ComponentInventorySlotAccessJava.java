package li.cil.oc.common.container;

import li.cil.oc.api.network.ManagedEnvironment;
import scala.Option;
import scala.Option$;

import java.util.Optional;

/** Java view over the Scala-compatible component slot storage. */
public final class ComponentInventorySlotAccessJava {
    private ComponentInventorySlotAccessJava() {}

    @SuppressWarnings("unchecked")
    public static Optional<ManagedEnvironment>[] snapshot(ComponentInventory inventory) {
        Option<ManagedEnvironment>[] slots = inventory.componentSlots();
        Optional<ManagedEnvironment>[] result = (Optional<ManagedEnvironment>[]) new Optional<?>[slots.length];
        for (int index = 0; index < slots.length; index++) {
            result[index] = slots[index].isDefined() ? Optional.of(slots[index].get()) : Optional.empty();
        }
        return result;
    }

    public static int size(ComponentInventory inventory) {
        return inventory.componentSlots().length;
    }

    public static Optional<ManagedEnvironment> get(ComponentInventory inventory, int slot) {
        Option<ManagedEnvironment>[] slots = inventory.componentSlots();
        if (slot < 0 || slot >= slots.length || slots[slot].isEmpty()) return Optional.empty();
        return Optional.of(slots[slot].get());
    }

    public static void set(ComponentInventory inventory, int slot, ManagedEnvironment value) {
        Option<ManagedEnvironment>[] slots = inventory.componentSlots();
        if (slot < 0 || slot >= slots.length) throw new IndexOutOfBoundsException(slot);
        slots[slot] = value == null ? Option$.MODULE$.empty() : Option$.MODULE$.apply(value);
    }
}
