package li.cil.oc.common.blockentity.traits;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import li.cil.oc.api.machine.Machine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ComputerRuntimeJavaTest {
    @Test
    public void detectsEitherMachineStateTransition() {
        assertTrue(ComputerRuntimeJava.hasStateChanged(false, false, true, false));
        assertTrue(ComputerRuntimeJava.hasStateChanged(true, false, true, true));
        assertFalse(ComputerRuntimeJava.hasStateChanged(true, true, true, true));
    }

    @Test
    public void stopsOnlyNonProxyMachinesAfterDisposal() {
        assertTrue(ComputerRuntimeJava.shouldStopMachine(true, false));
        assertFalse(ComputerRuntimeJava.shouldStopMachine(true, true));
        assertFalse(ComputerRuntimeJava.shouldStopMachine(false, false));
    }

    @Test
    public void serverSetChangedRefreshesMachineAndRedstoneOutputInOrder() {
        List<String> events = new ArrayList<>();
        ComputerRuntimeJava.afterSetChanged(computer(true, events));
        assertEquals(List.of("host", "output:true"), events);
    }

    @Test
    public void clientSetChangedDoesNotTouchMachineState() {
        List<String> events = new ArrayList<>();
        ComputerRuntimeJava.afterSetChanged(computer(false, events));
        assertEquals(List.of(), events);
    }

    private static Computer computer(boolean server, List<String> events) {
        Machine machine = (Machine) Proxy.newProxyInstance(Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class}, (proxy, method, args) -> {
                if (method.getName().equals("onHostChanged")) events.add("host");
                return null;
            });
        return (Computer) Proxy.newProxyInstance(Computer.class.getClassLoader(),
            new Class<?>[]{Computer.class}, (proxy, method, args) -> switch (method.getName()) {
                case "isServer" -> server;
                case "machine" -> machine;
                case "hasRedstoneCard" -> true;
                case "setOutputEnabled" -> { events.add("output:" + args[0]); yield null; }
                default -> null;
            });
    }
}
