package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.machine.Machine;
import li.cil.oc.common.blockentity.RobotProxy;

/** Server tick logic shared by computers built from the Scala trait mixin. */
public final class ComputerRuntimeJava {
    private ComputerRuntimeJava() { }

    public static void afterSetChanged(Computer computer) {
        if (!computer.isServer()) return;
        computer.machine().onHostChanged();
        computer.setOutputEnabled(computer.hasRedstoneCard());
    }

    public static void update(Computer computer) {
        // Wait one tick after loading for peer block entities to rejoin the
        // network, otherwise components such as GPUs can miss their screens.
        if (!computer.isServer() || !computer.computerNetworkConnected()) return;

        computer.updateComputer();
        boolean running = computer.machine().isRunning();
        boolean errored = computer.machine().lastError() != null;
        if (hasStateChanged(computer.isRunning(), computer.hasErrored(), running, errored)) {
            computer.setObservedRunningState(running, errored);
            computer.onRunningChanged();
        }

        computer.updateComponents();
    }

    public static void afterDispose(Computer computer) {
        Machine machine = computer.machine();
        if (shouldStopMachine(machine != null, computer instanceof RobotProxy)) machine.stop();
    }

    static boolean shouldStopMachine(boolean hasMachine, boolean robotProxy) {
        return hasMachine && !robotProxy;
    }

    static boolean hasStateChanged(boolean oldRunning, boolean oldErrored,
                                   boolean running, boolean errored) {
        return oldRunning != running || oldErrored != errored;
    }
}

