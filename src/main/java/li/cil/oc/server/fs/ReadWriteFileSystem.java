package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import scala.Option;

import java.io.File;

/** Capacity-limited storage that writes directly to disk. */
final class ReadWriteFileSystem extends CapacityFileSystem implements FileOutputStreamFileSystem {
    private final File root;

    ReadWriteFileSystem(File root, long capacity) {
        super(capacity);
        this.root = root;
        initializeAccounting();
    }

    ReadWriteFileSystem(File root, long capacity, long fileCost) {
        super(capacity);
        this.root = root;
        initializeAccounting(fileCost);
    }

    @Override public File root() { return root; }
    @Override protected boolean deleteStorage(String path) { return FileOutputStreamFileSystem.super.delete(path); }
    @Override protected boolean renameStorage(String from, String to) { return FileOutputStreamFileSystem.super.rename(from, to); }
    @Override protected boolean makeStorageDirectory(String path) { return FileOutputStreamFileSystem.super.makeDirectory(path); }
    @Override protected Option<OutputHandle> openStorageHandle(int id, String path, Mode mode) {
        return FileOutputFileSystemJava.openOutputHandle(this, root, id, path, mode);
    }
    @Override protected void saveStorage(CompoundTag nbt) {
        FileOutputFileSystemJava.saveData(root, () -> super.saveStorage(nbt));
    }
}
