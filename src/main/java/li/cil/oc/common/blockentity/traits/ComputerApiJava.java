package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Node;
import li.cil.oc.api.util.StateAware;

import java.util.EnumSet;

/** Java implementations of the small MachineHost and Analyzable API queries. */
public final class ComputerApiJava {
    private ComputerApiJava() { }

    public static Node node(Computer computer) {
        return computer.isServer() ? computer.machine().node() : null;
    }

    public static EnumSet<StateAware.State> currentState(boolean running) {
        return running
                ? EnumSet.of(StateAware.State.IsWorking)
                : EnumSet.noneOf(StateAware.State.class);
    }

    public static Node[] analyze(Computer computer) {
        return new Node[]{computer.machine().node()};
    }
}
