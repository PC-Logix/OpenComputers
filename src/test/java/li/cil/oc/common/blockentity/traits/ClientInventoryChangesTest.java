package li.cil.oc.common.blockentity.traits;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class ClientInventoryChangesTest {
    @Test
    public void coalescesAReplacementIntoRemoveThenAddAtTickBoundary() {
        ClientInventoryChanges<String> changes = new ClientInventoryChanges<>();
        List<Runnable> scheduled = new ArrayList<>();
        List<String> calls = new ArrayList<>();
        AtomicInteger size = new AtomicInteger(1);
        BiPredicate<String, String> matches = String::equals;

        changes.onItemRemoved(0, size::get, "old", matches,
                value -> value == null,
                (slot, stack) -> calls.add("remove:" + stack),
                (slot, stack) -> calls.add("add:" + stack),
                () -> calls.add("changed"), scheduled::add);
        changes.onItemAdded(0, size::get, "new", matches,
                value -> value == null,
                (slot, stack) -> calls.add("remove:" + stack),
                (slot, stack) -> calls.add("add:" + stack),
                () -> calls.add("changed"), scheduled::add);

        assertEquals(1, scheduled.size());
        assertEquals(0, calls.size());
        scheduled.getFirst().run();
        assertEquals(List.of("remove:old", "add:new", "changed"), calls);
    }

    @Test
    public void matchingReplacementCancelsAndFlushUsesCurrentInventorySize() {
        ClientInventoryChanges<String> changes = new ClientInventoryChanges<>();
        List<Runnable> scheduled = new ArrayList<>();
        List<String> calls = new ArrayList<>();
        AtomicInteger size = new AtomicInteger(2);
        BiPredicate<String, String> matches = String::equals;

        changes.onItemRemoved(0, size::get, "same", matches,
                value -> value == null,
                (slot, stack) -> calls.add("remove:" + stack),
                (slot, stack) -> calls.add("add:" + stack),
                () -> calls.add("changed"), scheduled::add);
        changes.onItemAdded(0, size::get, "same", matches,
                value -> value == null,
                (slot, stack) -> calls.add("remove:" + stack),
                (slot, stack) -> calls.add("add:" + stack),
                () -> calls.add("changed"), scheduled::add);
        scheduled.getFirst().run();
        assertEquals(0, calls.size());

        changes.onItemRemoved(1, size::get, "removed", matches,
                value -> value == null,
                (slot, stack) -> calls.add("remove:" + stack),
                (slot, stack) -> calls.add("add:" + stack),
                () -> calls.add("changed"), scheduled::add);
        size.set(1);
        scheduled.getLast().run();
        assertEquals(0, calls.size());
    }

    @Test
    public void emptyIncomingValueIsStoredAsNoPendingStack() {
        ClientInventoryChanges<String> changes = new ClientInventoryChanges<>();
        List<Runnable> scheduled = new ArrayList<>();
        List<String> calls = new ArrayList<>();
        BiPredicate<String, String> matches = (first, second) -> false;

        changes.onItemRemoved(0, () -> 1, "old", matches, value -> value == null,
                (slot, stack) -> calls.add("remove:" + stack),
                (slot, stack) -> calls.add("add:" + stack),
                () -> calls.add("changed"), scheduled::add);
        changes.onItemAdded(0, () -> 1, null, matches, value -> value == null,
                (slot, stack) -> calls.add("remove:" + stack),
                (slot, stack) -> calls.add("add:" + stack),
                () -> calls.add("changed"), scheduled::add);

        scheduled.getFirst().run();
        assertEquals(List.of("remove:old", "changed"), calls);
    }
}
