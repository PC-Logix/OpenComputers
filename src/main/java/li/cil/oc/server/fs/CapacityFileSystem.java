package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import scala.Option;

/** Applies capacity accounting around an explicitly selected storage implementation. */
abstract class CapacityFileSystem extends JavaOutputStreamFileSystem implements Capacity {
    private final long capacity;
    private CapacityAccounting accounting;

    CapacityFileSystem(long capacity) { this.capacity = capacity; }

    protected final void initializeAccounting() { accounting = new CapacityAccounting(this); }
    protected final void initializeAccounting(long fileCost) {
        accounting = new CapacityAccounting(this, fileCost, CapacityJava.computeSize(this, "/", fileCost));
    }

    @Override public final long capacity() { return capacity; }
    @Override public final CapacityAccounting accounting() { return accounting; }
    @Override public final long spaceTotal() { return capacity; }
    @Override public final long spaceUsed() { return accounting.used(); }

    @Override public final boolean delete(String path) {
        return CapacityOperations.delete(accounting, path, () -> deleteStorage(path));
    }
    @Override public final boolean rename(String from, String to) {
        return CapacityOperations.rename(this, accounting, from, to, () -> renameStorage(from, to));
    }
    @Override public final boolean makeDirectory(String path) {
        return CapacityOperations.makeDirectory(accounting, () -> makeStorageDirectory(path));
    }
    @Override public final Option<OutputHandle> openOutputHandle(int id, String path, Mode mode) {
        return CapacityOperations.open(this, accounting, path, mode, () -> openStorageHandle(id, path, mode));
    }
    @Override public final void close() { CapacityOperations.close(accounting, this::closeStorage); }
    @Override public final void loadData(CompoundTag nbt) {
        CapacityOperations.load(accounting, () -> loadStorage(nbt));
    }
    @Override public final void saveData(CompoundTag nbt) {
        CapacityOperations.save(accounting, nbt, () -> saveStorage(nbt));
    }

    protected abstract boolean deleteStorage(String path);
    protected abstract boolean renameStorage(String from, String to);
    protected abstract boolean makeStorageDirectory(String path);
    protected abstract Option<OutputHandle> openStorageHandle(int id, String path, Mode mode);
    protected void closeStorage() { super.close(); }
    protected void loadStorage(CompoundTag nbt) { super.loadData(nbt); }
    protected void saveStorage(CompoundTag nbt) { super.saveData(nbt); }
}
