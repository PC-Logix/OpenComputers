package li.cil.oc.server.fs;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory filesystem tree shared by volatile and buffered filesystems. */
public interface VirtualFileSystem extends OutputStreamFileSystem {
    VirtualDirectory root();
    default String[] segments(String path) { return FileSystemPathJava.segments(path); }

    @Override default boolean exists(String path) { return VirtualFileSystemJava.get(this, path).isPresent(); }
    @Override default boolean isDirectory(String path) {
        Optional<VirtualObject> value = VirtualFileSystemJava.get(this, path);
        return value.isPresent() && value.get().isDirectory();
    }
    @Override default long size(String path) {
        Optional<VirtualObject> value = VirtualFileSystemJava.get(this, path);
        return value.isPresent() ? value.get().size() : 0L;
    }
    @Override default long lastModified(String path) {
        Optional<VirtualObject> value = VirtualFileSystemJava.get(this, path);
        return value.isPresent() ? value.get().lastModified() : 0L;
    }
    @Override default String[] list(String path) {
        Optional<VirtualObject> value = VirtualFileSystemJava.get(this, path);
        return value.isPresent() && value.get() instanceof VirtualDirectory
                ? ((VirtualDirectory) value.get()).list() : null;
    }
    @Override default boolean delete(String path) { return VirtualFileSystemJava.delete(this, path); }
    @Override default boolean makeDirectory(String path) { return VirtualFileSystemJava.makeDirectory(this, path); }
    @Override default boolean rename(String from, String to) { return VirtualFileSystemJava.rename(this, from, to); }
    @Override default boolean setLastModified(String path, long time) {
        Optional<VirtualObject> value = VirtualFileSystemJava.get(this, path);
        if (value.isEmpty() || time < 0) return false;
        value.get().setLastModified(time);
        return true;
    }

    abstract class VirtualObject {
        private long lastModified = System.currentTimeMillis();
        public abstract boolean isDirectory();
        public abstract long size();
        public long lastModified() { return lastModified; }
        public void setLastModified(long value) { lastModified = value; }
        public abstract boolean canDelete();
        public void loadData(CompoundTag nbt) {
            if (nbt.contains("lastModified")) lastModified = nbt.getLong("lastModified");
        }
        public void saveData(CompoundTag nbt) { nbt.putLong("lastModified", lastModified); }
    }

    final class VirtualFile extends VirtualObject {
        private final List<Byte> data = new ArrayList<>();
        private Object handle;
        @Override public boolean isDirectory() { return false; }
        @Override public long size() { return data.size(); }
        public List<Byte> data() { return data; }
        public Object handle() { return handle; }
        public void setHandle(Object value) { handle = value; }
        public java.io.InputStream openInputStream() { return new VirtualFileInputStreamJava(data); }
        @Override public boolean canDelete() { return handle == null; }
        @Override public void loadData(CompoundTag nbt) {
            super.loadData(nbt);
            data.clear();
            for (byte value : nbt.getByteArray("data")) data.add(value);
        }
        @Override public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            byte[] bytes = new byte[data.size()];
            for (int i = 0; i < bytes.length; i++) bytes[i] = data.get(i);
            nbt.putByteArray("data", bytes);
        }
    }

    final class VirtualDirectory extends VirtualObject {
        private static final String CHILDREN_TAG = "children";
        private static final String IS_DIRECTORY_TAG = "isDirectory";
        private static final String NAME_TAG = "name";
        private final Map<String, VirtualObject> children = new HashMap<>();
        public Map<String, VirtualObject> children() { return children; }
        @Override public boolean isDirectory() { return true; }
        @Override public long size() { return 0; }
        @Override public boolean canDelete() { return children.isEmpty(); }
        public String[] list() {
            java.util.ArrayList<String> result = new java.util.ArrayList<>();
            for (Map.Entry<String, VirtualObject> child : children.entrySet()) {
                result.add(child.getKey() + (child.getValue().isDirectory() ? "/" : ""));
            }
            return result.toArray(String[]::new);
        }
        public boolean makeDirectory(String name) {
            if (children.containsKey(name)) return false;
            children.put(name, new VirtualDirectory());
            setLastModified(System.currentTimeMillis());
            return true;
        }
        public boolean delete(String name) {
            Optional<VirtualObject> child = Optional.ofNullable(children.get(name));
            if (child.isPresent() && child.get().canDelete()) {
                children.remove(name);
                setLastModified(System.currentTimeMillis());
                return true;
            }
            return false;
        }
        public Optional<VirtualFile> touch(String name) {
            VirtualObject child = children.get(name);
            if (child != null) return child instanceof VirtualFile
                    ? Optional.of((VirtualFile) child) : Optional.empty();
            VirtualFile file = new VirtualFile();
            children.put(name, file);
            setLastModified(System.currentTimeMillis());
            return Optional.of(file);
        }
        @Override public void loadData(CompoundTag nbt) {
            super.loadData(nbt);
            ListTag serialized = nbt.getList(CHILDREN_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < serialized.size(); i++) {
                CompoundTag childNbt = serialized.getCompound(i);
                VirtualObject child = childNbt.getBoolean(IS_DIRECTORY_TAG) ? new VirtualDirectory() : new VirtualFile();
                child.loadData(childNbt);
                children.put(childNbt.getString(NAME_TAG), child);
            }
        }
        @Override public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            ListTag serialized = new ListTag();
            for (Map.Entry<String, VirtualObject> entry : children.entrySet()) {
                CompoundTag childNbt = new CompoundTag();
                childNbt.putBoolean(IS_DIRECTORY_TAG, entry.getValue().isDirectory());
                childNbt.putString(NAME_TAG, entry.getKey());
                entry.getValue().saveData(childNbt);
                serialized.add(childNbt);
            }
            nbt.put(CHILDREN_TAG, serialized);
        }
    }
}
