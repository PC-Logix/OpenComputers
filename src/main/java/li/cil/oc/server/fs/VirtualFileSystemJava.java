package li.cil.oc.server.fs;

import scala.Option;
import scala.Option$;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import li.cil.oc.api.fs.Mode;

/** Java path traversal and mutation operations for the in-memory filesystem tree. */
public final class VirtualFileSystemJava {
    private VirtualFileSystemJava() { }

    public static Optional<VirtualFileSystem.VirtualObject> get(VirtualFileSystem filesystem, String path) {
        String[] parts = filesystem.segments(path);
        VirtualFileSystem.VirtualObject current = filesystem.root();
        for (String part : parts) {
            if (!(current instanceof VirtualFileSystem.VirtualDirectory)) return Optional.empty();
            current = ((VirtualFileSystem.VirtualDirectory) current).children().get(part);
            if (current == null) return Optional.empty();
        }
        return Optional.of(current);
    }

    /** Resolve existing path components using the same case-folding behavior as buffered disk filesystems. */
    public static String[] toCaseInsensitive(VirtualFileSystem.VirtualDirectory root, String[] path) {
        String[] result = new String[path.length];
        VirtualFileSystem.VirtualDirectory directory = root;
        for (int i = 0; i < path.length; i++) {
            if (directory == null) throw new AssertionError("corrupted virtual file system");
            String segment = path[i];
            Iterator<Map.Entry<String, VirtualFileSystem.VirtualObject>> children = directory.children().entrySet().iterator();
            while (children.hasNext()) {
                Map.Entry<String, VirtualFileSystem.VirtualObject> child = children.next();
                if (child.getKey().toLowerCase().equals(segment.toLowerCase())) {
                    result[i] = child.getKey();
                    directory = child.getValue() instanceof VirtualFileSystem.VirtualDirectory
                            ? (VirtualFileSystem.VirtualDirectory) child.getValue() : null;
                    break;
                }
            }
            if (result[i] == null) result[i] = segment;
        }
        return result;
    }

    public static Option<InputChannel> openInputChannel(VirtualFileSystem filesystem, String path) {
        Optional<VirtualFileSystem.VirtualObject> value = get(filesystem, path);
        if (value.isPresent() && value.get() instanceof VirtualFileSystem.VirtualFile) {
            VirtualFileSystem.VirtualFile file = (VirtualFileSystem.VirtualFile) value.get();
            return Option$.MODULE$.apply(new InputStreamChannel(file.openInputStream()));
        }
        return Option$.MODULE$.empty();
    }

    public static Option<OutputHandle> openOutputHandle(VirtualFileSystem filesystem, int id, String path, Mode mode) {
        String[] parts = filesystem.segments(path);
        if (parts.length == 0) return Option$.MODULE$.empty();
        Optional<VirtualFileSystem.VirtualObject> parent = get(filesystem, join(parts, parts.length - 1));
        if (parent.isEmpty() || !(parent.get() instanceof VirtualFileSystem.VirtualDirectory))
            return Option$.MODULE$.empty();

        Optional<VirtualFileSystem.VirtualFile> touched = ((VirtualFileSystem.VirtualDirectory) parent.get()).touch(parts[parts.length - 1]);
        if (touched.isEmpty()) return Option$.MODULE$.empty();
        VirtualFileSystem.VirtualFile file = touched.get();
        if (file.handle() != null) return Option$.MODULE$.empty();
        if (mode.isTruncate()) {
            file.data().clear();
            file.setLastModified(System.currentTimeMillis());
        }
        VirtualOutputHandle handle = new VirtualOutputHandle(file, filesystem, id, path, mode);
        file.setHandle(handle);
        return Option$.MODULE$.apply(handle);
    }

    public static void loadData(VirtualFileSystem filesystem, CompoundTag nbt, Runnable fallback) {
        if (nbt.contains("root", Tag.TAG_COMPOUND)) filesystem.root().loadData(nbt.getCompound("root"));
        if (!(filesystem instanceof Buffered)) filesystem.root().loadData(nbt);
        fallback.run();
    }

    public static void saveData(VirtualFileSystem filesystem, CompoundTag nbt, Runnable fallback) {
        fallback.run();
        if (!(filesystem instanceof Buffered)) {
            CompoundTag data = new CompoundTag();
            filesystem.root().saveData(data);
            nbt.put("root", data);
        }
    }

    public static boolean delete(VirtualFileSystem filesystem, String path) {
        String[] parts = filesystem.segments(path);
        if (parts.length == 0) return true;
        Optional<VirtualFileSystem.VirtualObject> parent = get(filesystem, join(parts, parts.length - 1));
        return parent.isPresent() && parent.get() instanceof VirtualFileSystem.VirtualDirectory
                && ((VirtualFileSystem.VirtualDirectory) parent.get()).delete(parts[parts.length - 1]);
    }

    public static boolean makeDirectory(VirtualFileSystem filesystem, String path) {
        String[] parts = filesystem.segments(path);
        if (parts.length == 0) return false;
        Optional<VirtualFileSystem.VirtualObject> parent = get(filesystem, join(parts, parts.length - 1));
        return parent.isPresent() && parent.get() instanceof VirtualFileSystem.VirtualDirectory
                && ((VirtualFileSystem.VirtualDirectory) parent.get()).makeDirectory(parts[parts.length - 1]);
    }

    public static boolean rename(VirtualFileSystem filesystem, String from, String to) {
        if (from.isEmpty() || get(filesystem, from).isEmpty())
            return VirtualFileSystemJava.<RuntimeException, Boolean>throwUnchecked(new FileNotFoundException(from));
        String[] destination = filesystem.segments(to);
        if (destination.length == 0) return false;
        Optional<VirtualFileSystem.VirtualObject> targetParent = get(filesystem, join(destination, destination.length - 1));
        if (targetParent.isEmpty() || !(targetParent.get() instanceof VirtualFileSystem.VirtualDirectory)) return false;

        String[] source = filesystem.segments(from);
        Optional<VirtualFileSystem.VirtualObject> sourceParent = get(filesystem, join(source, source.length - 1));
        VirtualFileSystem.VirtualDirectory sourceDirectory = (VirtualFileSystem.VirtualDirectory) sourceParent.get();
        VirtualFileSystem.VirtualDirectory destinationDirectory = (VirtualFileSystem.VirtualDirectory) targetParent.get();
        String sourceName = source[source.length - 1];
        String destinationName = destination[destination.length - 1];
        VirtualFileSystem.VirtualObject object = sourceDirectory.children().get(sourceName);

        if (destinationDirectory.children().containsKey(destinationName)) destinationDirectory.delete(destinationName);
        sourceDirectory.children().remove(sourceName);
        sourceDirectory.setLastModified(System.currentTimeMillis());
        destinationDirectory.children().put(destinationName, object);
        destinationDirectory.setLastModified(System.currentTimeMillis());
        object.setLastModified(System.currentTimeMillis());
        return true;
    }

    private static String join(String[] parts, int count) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < count; index++) result.append(parts[index]).append('/');
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T throwUnchecked(Throwable error) throws E { throw (E) error; }
}
