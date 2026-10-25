package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import scala.Option;

/** Shared Java tree and stream composition for RAM and buffered storage. */
abstract class VirtualCapacityFileSystem extends CapacityFileSystem implements VirtualFileSystem {
    private final VirtualDirectory root = new VirtualDirectory();

    VirtualCapacityFileSystem(long capacity) { super(capacity); }

    @Override public final VirtualDirectory root() { return root; }
    @Override public Option<InputChannel> openInputChannel(String path) {
        return VirtualFileSystemJava.openInputChannel(this, path);
    }
    @Override protected Option<OutputHandle> openStorageHandle(int id, String path, Mode mode) {
        return VirtualFileSystemJava.openOutputHandle(this, id, path, mode);
    }
    @Override protected boolean deleteStorage(String path) { return VirtualFileSystem.super.delete(path); }
    @Override protected boolean renameStorage(String from, String to) { return VirtualFileSystem.super.rename(from, to); }
    @Override protected boolean makeStorageDirectory(String path) { return VirtualFileSystem.super.makeDirectory(path); }
    @Override protected void loadStorage(CompoundTag nbt) {
        VirtualFileSystemJava.loadData(this, nbt, () -> super.loadStorage(nbt));
    }
    @Override protected void saveStorage(CompoundTag nbt) {
        VirtualFileSystemJava.saveData(this, nbt, () -> super.saveStorage(nbt));
    }
}
