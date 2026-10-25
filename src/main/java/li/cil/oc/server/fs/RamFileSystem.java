package li.cil.oc.server.fs;

/** Volatile capacity-limited storage used for computer temporary files. */
final class RamFileSystem extends VirtualCapacityFileSystem implements Volatile {
    RamFileSystem(long capacity) {
        super(capacity);
        initializeAccounting();
    }

    RamFileSystem(long capacity, long fileCost) {
        super(capacity);
        initializeAccounting(fileCost);
    }

    @Override protected void closeStorage() {
        super.closeStorage();
        root().children().clear();
    }
}
