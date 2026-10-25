package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.SidedEnvironment;

/** Java balancing capability; ticking is dispatched at the host's explicit lifecycle phase. */
public interface PowerBalancer extends PowerInformation, SidedEnvironment, Tickable {
    PowerBalancerStateJava powerBalancerState();
    boolean isServer();
    boolean powerNetworkConnected();
    default boolean powerBalanceAfterHub() { return false; }

    @Override default PowerInformationJava powerInformationState() { return powerBalancerState().information(); }
    @Override default double globalBuffer() { return powerBalancerState().globalBuffer(); }
    @Override default void globalBuffer_$eq(double value) { powerBalancerState().setGlobalBuffer(value); }
    @Override default double globalBufferSize() { return powerBalancerState().globalBufferSize(); }
    @Override default void globalBufferSize_$eq(double value) { powerBalancerState().setGlobalBufferSize(value); }
    default double[] distributeJava() { return PowerBalancerJava.distribute(this); }

    /** Scala tuple compatibility accessor. */
    default scala.Tuple2<Object, Object> distribute() {
        double[] totals = distributeJava();
        return new scala.Tuple2<>(totals[0], totals[1]);
    }
}
