package li.cil.oc.common.blockentity.traits;

/** Mutable machine, execution, ownership, and deferred-load state owned by each computer host. */
public final class ComputerStateJava {
    public final ComputerMachineStateJava machine = new ComputerMachineStateJava();
    public final ComputerExecutionStateJava execution = new ComputerExecutionStateJava();
    public final ComputerMachineLoadState load = new ComputerMachineLoadState();
    public final ComputerComponentAccessJava components = new ComputerComponentAccessJava();
}
