package li.cil.oc.common.blockentity.traits;

/** Owns the requested and observed machine-running flags exposed by computer hosts. */
public final class ComputerExecutionStateJava {
    private boolean running;
    private boolean errored;

    public boolean isRunning() { return running; }
    public boolean hasErrored() { return errored; }
    public void setHasErrored(boolean value) { errored = value; }

    public void requestRunning(boolean value) {
        running = value;
        if (value) errored = false;
    }

    public void observe(boolean running, boolean errored) {
        this.running = running;
        this.errored = errored;
    }
}
