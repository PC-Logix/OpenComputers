package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import scala.Option;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Java-owned open-handle state and persistence for writable filesystems. */
public final class OutputStreamFileSystemSupport {
    private static final String OUTPUT_TAG = "output";
    private static final String HANDLE_TAG = "handle";
    private static final String PATH_TAG = "path";
    private static final String MODE_TAG = "mode";
    private static final String POSITION_TAG = "position";
    private static final Map<OutputStreamFileSystem, State> STATES = new WeakHashMap<>();

    private OutputStreamFileSystemSupport() { }

    private static State state(OutputStreamFileSystem owner) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(owner, ignored -> new State());
        }
    }

    public static int open(OutputStreamFileSystem owner, String path, Mode mode) throws FileNotFoundException {
        synchronized (owner) {
            FileSystemPathJava.validatePath(path);
            if (owner.isDirectory(path) || (mode.requiresExisting() && !owner.exists(path))) {
                throw new FileNotFoundException(path);
            }
            State state = state(owner);
            int handle;
            do {
                handle = (int) (Math.random() * Integer.MAX_VALUE) + 1;
            } while (state.handles.containsKey(handle));

            Option<OutputHandle> opened = owner.openOutputHandle(handle, path, mode);
            if (opened != null && opened.isDefined()) {
                state.handles.put(handle, opened.get());
                return handle;
            }
            throw new FileNotFoundException(path);
        }
    }

    public static li.cil.oc.api.fs.Handle getHandle(OutputStreamFileSystem owner, int handle) {
        synchronized (owner) {
            return state(owner).handles.get(handle);
        }
    }

    public static void removeOutputHandle(OutputStreamFileSystem owner, int handle) {
        synchronized (owner) {
            state(owner).handles.remove(handle);
        }
    }

    public static void close(OutputStreamFileSystem owner) {
        synchronized (owner) {
            State state = state(owner);
            for (OutputHandle handle : new ArrayList<>(state.handles.values())) handle.close();
            state.handles.clear();
        }
    }

    public static void loadData(OutputStreamFileSystem owner, CompoundTag nbt) {
        synchronized (owner) {
            ListTag savedHandles = nbt.getList(OUTPUT_TAG, Tag.TAG_COMPOUND);
            for (int index = 0; index < savedHandles.size(); index++) {
                CompoundTag saved = savedHandles.getCompound(index);
                int handle = saved.getInt(HANDLE_TAG);
                String path = saved.getString(PATH_TAG);
                Mode mode;
                if (saved.contains(MODE_TAG, Tag.TAG_STRING)) {
                    try { mode = Mode.valueOf(saved.getString(MODE_TAG)); }
                    catch (IllegalArgumentException ignored) { mode = Mode.Append; }
                } else {
                    mode = Mode.Append;
                }
                long position = saved.contains(POSITION_TAG) ? saved.getLong(POSITION_TAG) : -1L;
                Mode reopenMode = mode == Mode.Write ? Mode.Append
                        : mode == Mode.ReadWriteTruncate ? Mode.ReadWrite : mode;
                Option<OutputHandle> opened = owner.openOutputHandle(handle, path, reopenMode);
                if (opened != null && opened.isDefined()) {
                    OutputHandle fileHandle = opened.get();
                    fileHandle.restoreMode(mode);
                    if (position >= 0) seek(fileHandle, position);
                    state(owner).handles.put(handle, fileHandle);
                }
            }
        }
    }

    public static void saveData(OutputStreamFileSystem owner, CompoundTag nbt) {
        synchronized (owner) {
            ListTag savedHandles = new ListTag();
            for (OutputHandle file : state(owner).handles.values()) {
                assert !file.isClosed();
                CompoundTag saved = new CompoundTag();
                saved.putInt(HANDLE_TAG, file.handle());
                saved.putString(PATH_TAG, file.path());
                saved.putString(MODE_TAG, file.mode().name());
                saved.putLong(POSITION_TAG, file.position());
                savedHandles.add(saved);
            }
            nbt.put(OUTPUT_TAG, savedHandles);
        }
    }

    private static void seek(OutputHandle handle, long position) {
        try { handle.seek(position); }
        catch (IOException error) { throw sneaky(error); }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable error) throws T {
        throw (T) error;
    }

    private static final class State {
        private final Map<Integer, OutputHandle> handles = new HashMap<>();
    }
}
