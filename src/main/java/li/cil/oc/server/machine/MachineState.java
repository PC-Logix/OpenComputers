package li.cil.oc.server.machine;

/** Possible states of the computer and its executor. */
public enum MachineState {
    /** The computer is not running right now and there is no Lua state. */
    Stopped,
    /** Booting up, doing the first run to initialize the kernel and libraries. */
    Starting,
    /** The computer is currently rebooting. */
    Restarting,
    /** The computer is currently shutting down. */
    Stopping,
    /** The computer is paused and waiting for the game to resume. */
    Paused,
    /** The executor is waiting for a synchronized call to be made. */
    SynchronizedCall,
    /** The computer should resume with the result of a synchronized call. */
    SynchronizedReturn,
    /** The computer will resume as soon as possible. */
    Yielded,
    /** The computer is yielding for a longer amount of time. */
    Sleeping,
    /** The computer is up and running, executing Lua code. */
    Running
}
