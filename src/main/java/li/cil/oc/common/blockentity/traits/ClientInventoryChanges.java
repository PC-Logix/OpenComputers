package li.cil.oc.common.blockentity.traits;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/** Coalesces client-side inventory slot callbacks until the next client tick. */
public final class ClientInventoryChanges<T> {
    private final ArrayList<T> pendingRemovals = new ArrayList<>();
    private final ArrayList<T> pendingAdds = new ArrayList<>();
    private boolean updateScheduled;

    public void onItemAdded(int slot, IntSupplier size, T stack, BiPredicate<T, T> matches, Predicate<T> isEmpty,
                            BiConsumer<Integer, T> remove, BiConsumer<Integer, T> add,
                            Runnable changed, Consumer<Runnable> schedule) {
        adjustSize(size.getAsInt());
        T removed = pendingRemovals.get(slot);
        if (removed != null && matches.test(removed, stack)) {
            // The client reverted to the original stack before deferred work ran.
            pendingAdds.set(slot, null);
            pendingRemovals.set(slot, null);
        } else {
            pendingAdds.set(slot, isEmpty.test(stack) ? null : stack);
            scheduleIfNeeded(size, matches, remove, add, changed, schedule);
        }
    }

    public void onItemRemoved(int slot, IntSupplier size, T stack, BiPredicate<T, T> matches, Predicate<T> isEmpty,
                              BiConsumer<Integer, T> remove, BiConsumer<Integer, T> add,
                              Runnable changed, Consumer<Runnable> schedule) {
        adjustSize(size.getAsInt());
        if (pendingAdds.get(slot) != null) {
            pendingAdds.set(slot, null);
        } else if (pendingRemovals.get(slot) == null) {
            pendingRemovals.set(slot, isEmpty.test(stack) ? null : stack);
            scheduleIfNeeded(size, matches, remove, add, changed, schedule);
        }
    }

    private void scheduleIfNeeded(IntSupplier size, BiPredicate<T, T> matches,
                                  BiConsumer<Integer, T> remove, BiConsumer<Integer, T> add,
                                  Runnable changed, Consumer<Runnable> schedule) {
        if (updateScheduled) return;
        updateScheduled = true;
        schedule.accept(() -> apply(size.getAsInt(), matches, remove, add, changed));
    }

    private void apply(int size, BiPredicate<T, T> matches,
                       BiConsumer<Integer, T> remove, BiConsumer<Integer, T> add,
                       Runnable changed) {
        updateScheduled = false;
        adjustSize(size);
        for (int slot = 0; slot < size; slot++) {
            T removed = pendingRemovals.get(slot);
            T added = pendingAdds.get(slot);
            if (removed != null && added != null) {
                if (!matches.test(removed, added)) {
                    remove.accept(slot, removed);
                    add.accept(slot, added);
                    changed.run();
                }
            } else if (removed != null) {
                remove.accept(slot, removed);
                changed.run();
            } else if (added != null) {
                add.accept(slot, added);
                changed.run();
            }

            pendingRemovals.set(slot, null);
            pendingAdds.set(slot, null);
        }
    }

    private void adjustSize(int size) {
        while (pendingRemovals.size() > size) pendingRemovals.remove(pendingRemovals.size() - 1);
        while (pendingAdds.size() > size) pendingAdds.remove(pendingAdds.size() - 1);
        while (pendingRemovals.size() < size) pendingRemovals.add(null);
        while (pendingAdds.size() < size) pendingAdds.add(null);
    }
}
