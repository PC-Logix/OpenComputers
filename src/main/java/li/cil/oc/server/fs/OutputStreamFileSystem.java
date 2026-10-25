package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import scala.Option;

import java.io.FileNotFoundException;

/** Writable streams with explicit input-before-output lifecycle order. */
public interface OutputStreamFileSystem extends InputStreamFileSystem, OutputHandleOwner {
    @Override default boolean isReadOnly() { return false; }
    @Override default int open(String path, Mode mode) {
        synchronized (this) {
            if (!mode.isWritable()) return InputStreamFileSystem.super.open(path, mode);
            try { return OutputStreamFileSystemSupport.open(this, path, mode); }
            catch (FileNotFoundException error) { throw unchecked(error); }
        }
    }
    @Override default Handle getHandle(int handle) {
        synchronized (this) {
            Handle input = InputStreamFileSystem.super.getHandle(handle);
            return input != null ? input : OutputStreamFileSystemSupport.getHandle(this, handle);
        }
    }
    @Override default void close() {
        synchronized (this) {
            InputStreamFileSystem.super.close();
            OutputStreamFileSystemSupport.close(this);
        }
    }
    @Override default void loadData(CompoundTag nbt) {
        InputStreamFileSystem.super.loadData(nbt);
        OutputStreamFileSystemSupport.loadData(this, nbt);
    }
    @Override default void saveData(CompoundTag nbt) {
        synchronized (this) {
            InputStreamFileSystem.super.saveData(nbt);
            OutputStreamFileSystemSupport.saveData(this, nbt);
        }
    }
    Option<OutputHandle> openOutputHandle(int id, String path, Mode mode);
    @Override default void removeOutputHandle(int handle) { OutputStreamFileSystemSupport.removeOutputHandle(this, handle); }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException unchecked(Throwable error) throws T { throw (T) error; }
}
