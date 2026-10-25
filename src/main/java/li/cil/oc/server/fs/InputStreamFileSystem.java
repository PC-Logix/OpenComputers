package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import scala.Option;

import java.io.FileNotFoundException;

/** Read-only stream filesystem contract, backed by Java handle management. */
public interface InputStreamFileSystem extends li.cil.oc.api.fs.FileSystem {
    @Override default boolean isReadOnly() { return true; }
    @Override default boolean delete(String path) { return false; }
    @Override default boolean makeDirectory(String path) { return false; }
    @Override default boolean rename(String from, String to) { return false; }
    @Override default boolean setLastModified(String path, long time) { return false; }
    @Override default int open(String path, Mode mode) {
        try { return InputStreamFileSystemSupport.open(this, path, mode); }
        catch (FileNotFoundException error) { throw unchecked(error); }
    }
    @Override default Handle getHandle(int handle) { return InputStreamFileSystemSupport.getHandle(this, handle); }
    @Override default void close() { InputStreamFileSystemSupport.close(this); }
    @Override default void loadData(CompoundTag nbt) { InputStreamFileSystemSupport.loadData(this, nbt); }
    @Override default void saveData(CompoundTag nbt) { InputStreamFileSystemSupport.saveData(this, nbt); }
    Option<InputChannel> openInputChannel(String path);

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException unchecked(Throwable error) throws T { throw (T) error; }
}
