package li.cil.oc.common.blockentity.traits;

/** Stores the last network-wide energy values reported by a power balancer. */
public final class PowerBalancerStateJava {
    private final PowerInformationJava information = new PowerInformationJava();
    private double globalBuffer;
    private double globalBufferSize;

    public PowerInformationJava information() { return information; }
    public double globalBuffer() { return globalBuffer; }
    public void setGlobalBuffer(double value) { globalBuffer = value; }
    public double globalBufferSize() { return globalBufferSize; }
    public void setGlobalBufferSize(double value) { globalBufferSize = value; }
}
