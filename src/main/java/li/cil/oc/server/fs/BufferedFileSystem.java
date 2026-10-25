package li.cil.oc.server.fs;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;

import java.io.File;
import java.io.IOException;
import java.util.function.BooleanSupplier;

/** Capacity-limited virtual storage mirrored to disk through the save executor. */
final class BufferedFileSystem extends VirtualCapacityFileSystem implements Buffered {
    private final File fileRoot;
    private final BufferedPersistence persistence = new BufferedPersistence();
    private final BooleanSupplier caseInsensitive;

    BufferedFileSystem(File fileRoot, long capacity) {
        super(capacity);
        this.fileRoot = fileRoot;
        caseInsensitive = FileSystem::isCaseInsensitive;
        initializeAccounting();
    }

    BufferedFileSystem(File fileRoot, long capacity, long fileCost, BooleanSupplier caseInsensitive) {
        super(capacity);
        this.fileRoot = fileRoot;
        this.caseInsensitive = caseInsensitive;
        initializeAccounting(fileCost);
    }

    @Override public File fileRoot() { return fileRoot; }
    @Override public BufferedPersistence persistence() { return persistence; }
    @Override public String[] segments(String path) {
        String[] parts = FileSystemPathJava.segments(path);
        return caseInsensitive.getAsBoolean() ? VirtualFileSystemJava.toCaseInsensitive(root(), parts) : parts;
    }
    @Override protected boolean deleteStorage(String path) {
        return persistence.delete(path, () -> super.deleteStorage(path));
    }
    @Override protected boolean renameStorage(String from, String to) {
        return persistence.rename(from, to, () -> super.renameStorage(from, to));
    }
    @Override protected void loadStorage(CompoundTag nbt) {
        persistence.waitForSaving(message -> OpenComputers.log().warn(message));
        persistence.loadFiles(this, this::restoreFile);
        super.loadStorage(nbt);
    }
    private boolean restoreFile(String path, File file) {
        try {
            return BufferedFileOperations.restore(file, () -> openOutputHandle(0, path, Mode.Write));
        } catch (IOException error) {
            throw unchecked(error);
        }
    }
    @Override protected void saveStorage(CompoundTag nbt) {
        super.saveStorage(nbt);
        persistence.scheduleSave(Buffered.fileSaveHandler(), this::saveFiles);
    }
    public void saveFiles() { persistence.saveFiles(this, this::storeFile); }
    private boolean storeFile(String path, File file, long modified) {
        try {
            return BufferedFileOperations.store(file, modified, () -> openInputChannel(path));
        } catch (IOException error) {
            throw unchecked(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException unchecked(Throwable error) throws T { throw (T) error; }
}
