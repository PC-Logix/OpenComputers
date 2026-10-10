package li.cil.oc.server.fs;

import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;

import java.io.FileNotFoundException;

final class ReadOnlyWrapper implements FileSystem {
    private final FileSystem fileSystem;

    ReadOnlyWrapper(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public FileSystem fileSystem() {
        return fileSystem;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public long spaceTotal() {
        return fileSystem.spaceUsed();
    }

    @Override
    public long spaceUsed() {
        return fileSystem.spaceUsed();
    }

    @Override
    public boolean exists(String path) {
        return fileSystem.exists(path);
    }

    @Override
    public long size(String path) {
        return fileSystem.size(path);
    }

    @Override
    public boolean isDirectory(String path) {
        return fileSystem.isDirectory(path);
    }

    @Override
    public long lastModified(String path) {
        return fileSystem.lastModified(path);
    }

    @Override
    public String[] list(String path) {
        return fileSystem.list(path);
    }

    @Override
    public boolean delete(String path) {
        return false;
    }

    @Override
    public boolean makeDirectory(String path) {
        return false;
    }

    @Override
    public boolean rename(String from, String to) {
        return false;
    }

    @Override
    public boolean setLastModified(String path, long time) {
        return false;
    }

    @Override
    public int open(String path, Mode mode) throws FileNotFoundException {
        if (!mode.isWritable()) {
            return fileSystem.open(path, mode);
        }
        throw new FileNotFoundException("read-only filesystem; cannot open for writing: " + path);
    }

    @Override
    public Handle getHandle(int handle) {
        return fileSystem.getHandle(handle);
    }

    @Override
    public void close() {
        fileSystem.close();
    }

    @Override
    public void loadData(CompoundTag nbt) {
        fileSystem.loadData(nbt);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        fileSystem.saveData(nbt);
    }
}
