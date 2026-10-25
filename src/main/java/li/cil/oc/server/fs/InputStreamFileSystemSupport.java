package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import scala.Option;

import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Java-owned operations and state for read-only stream-backed filesystems. */
public final class InputStreamFileSystemSupport {
    private static final String INPUT_TAG = "input";
    private static final String HANDLE_TAG = "handle";
    private static final String PATH_TAG = "path";
    private static final String POSITION_TAG = "position";
    private static final Map<InputStreamFileSystem, State> STATES = new WeakHashMap<>();

    private InputStreamFileSystemSupport() { }

    private static State state(InputStreamFileSystem owner) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(owner, ignored -> new State());
        }
    }

    public static int open(InputStreamFileSystem owner, String path, Mode mode) throws FileNotFoundException {
        synchronized (owner) {
            FileSystemPathJava.validatePath(path);
            State state = state(owner);
            if (mode != Mode.Read || !owner.exists(path) || owner.isDirectory(path)) {
                throw new FileNotFoundException(path);
            }

            int handle;
            do {
                handle = (int) (Math.random() * Integer.MAX_VALUE) + 1;
            } while (state.handles.containsKey(handle));

            final int fileHandleId = handle;
            Option<InputChannel> opened = owner.openInputChannel(path);
            if (opened != null && opened.isDefined()) {
                WeakReference<InputStreamFileSystem> ownerReference = new WeakReference<>(owner);
                InputStreamHandleJava fileHandle = new InputStreamHandleJava(handle, path, opened.get(), value -> {
                    InputStreamFileSystem currentOwner = ownerReference.get();
                    return currentOwner == null ? 0 : currentOwner.size(value);
                }, () -> state.handles.remove(fileHandleId));
                state.handles.put(handle, fileHandle);
                return handle;
            }
            throw new FileNotFoundException(path);
        }
    }

    public static li.cil.oc.api.fs.Handle getHandle(InputStreamFileSystem owner, int handle) {
        synchronized (owner) {
            return state(owner).handles.get(handle);
        }
    }

    public static void close(InputStreamFileSystem owner) {
        synchronized (owner) {
            State state = state(owner);
            for (InputStreamHandleJava handle : new ArrayList<>(state.handles.values())) handle.close();
            state.handles.clear();
        }
    }

    public static void loadData(InputStreamFileSystem owner, CompoundTag nbt) {
        synchronized (owner) {
            State state = state(owner);
            ListTag savedHandles = nbt.getList(INPUT_TAG, Tag.TAG_COMPOUND);
            for (int index = 0; index < savedHandles.size(); index++) {
                CompoundTag saved = savedHandles.getCompound(index);
                int handle = saved.getInt(HANDLE_TAG);
                String path = saved.getString(PATH_TAG);
                long position = saved.getLong(POSITION_TAG);
                final int fileHandleId = handle;
                Option<InputChannel> opened = owner.openInputChannel(path);
                if (opened != null && opened.isDefined()) {
                    WeakReference<InputStreamFileSystem> ownerReference = new WeakReference<>(owner);
                    InputStreamHandleJava fileHandle = new InputStreamHandleJava(handle, path, opened.get(), value -> {
                        InputStreamFileSystem currentOwner = ownerReference.get();
                        return currentOwner == null ? 0 : currentOwner.size(value);
                    }, () -> state.handles.remove(fileHandleId));
                    seek(fileHandle, position);
                    state.handles.put(handle, fileHandle);
                }
            }
        }
    }

    public static void saveData(InputStreamFileSystem owner, CompoundTag nbt) {
        synchronized (owner) {
            ListTag savedHandles = new ListTag();
            for (InputStreamHandleJava file : state(owner).handles.values()) {
                assert file.channel().isOpen();
                CompoundTag saved = new CompoundTag();
                saved.putInt(HANDLE_TAG, file.handle());
                saved.putString(PATH_TAG, file.path());
                saved.putLong(POSITION_TAG, file.position());
                savedHandles.add(saved);
            }
            nbt.put(INPUT_TAG, savedHandles);
        }
    }

    private static void seek(InputStreamHandleJava handle, long position) {
        try {
            handle.seek(position);
        } catch (java.io.IOException error) {
            throw sneaky(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable error) throws T {
        throw (T) error;
    }

    private static final class State {
        private final Map<Integer, InputStreamHandleJava> handles = new HashMap<>();
    }
}
