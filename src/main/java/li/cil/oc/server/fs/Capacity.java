package li.cil.oc.server.fs;

/** Capacity-limited filesystem behavior shared by file and virtual storage. */
public interface Capacity extends OutputStreamFileSystem {
    long capacity();

    CapacityAccounting accounting();

    @Override default long spaceTotal() { return capacity(); }

    @Override default long spaceUsed() { return accounting().used(); }
}
