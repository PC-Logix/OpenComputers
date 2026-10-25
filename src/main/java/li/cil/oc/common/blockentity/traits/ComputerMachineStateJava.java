package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.machine.Machine;

import java.util.function.Supplier;

/** Owns the thread-safe, one-time server machine initialization used by computer hosts. */
public final class ComputerMachineStateJava {
    private boolean initialized;
    private Machine machine;

    public synchronized Machine get(boolean server, Supplier<Machine> factory) {
        if (!initialized) {
            machine = server ? factory.get() : null;
            initialized = true;
        }
        return machine;
    }
}
