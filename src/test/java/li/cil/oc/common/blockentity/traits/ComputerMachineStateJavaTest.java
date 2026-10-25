package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.machine.Machine;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class ComputerMachineStateJavaTest {
    @Test
    public void createsServerMachineOnlyOnce() {
        ComputerMachineStateJava state = new ComputerMachineStateJava();
        AtomicInteger calls = new AtomicInteger();
        Machine machine = machine();

        assertSame(machine, state.get(true, () -> { calls.incrementAndGet(); return machine; }));
        assertSame(machine, state.get(true, () -> { calls.incrementAndGet(); return machine; }));
        assertEquals(1, calls.get());
    }

    @Test
    public void cachesClientNullWithoutCallingFactory() {
        ComputerMachineStateJava state = new ComputerMachineStateJava();

        assertNull(state.get(false, () -> { fail("client hosts must not create a machine"); return null; }));
        assertNull(state.get(true, () -> { fail("lazy initialization result must be cached"); return machine(); }));
    }

    @Test
    public void retriesAfterFactoryFailureLikeScalaLazyInitialization() {
        ComputerMachineStateJava state = new ComputerMachineStateJava();
        try {
            state.get(true, () -> { throw new IllegalStateException("initialization failed"); });
            fail("expected initialization failure");
        } catch (IllegalStateException expected) {
            assertEquals("initialization failed", expected.getMessage());
        }
        Machine machine = machine();
        assertSame(machine, state.get(true, () -> machine));
    }

    private static Machine machine() {
        return (Machine) Proxy.newProxyInstance(Machine.class.getClassLoader(), new Class<?>[]{Machine.class},
                (proxy, method, arguments) -> null);
    }
}
