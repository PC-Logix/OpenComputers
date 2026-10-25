package li.cil.oc.server.fs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import scala.Option;
import scala.Option$;
import scala.Tuple2;
import scala.collection.immutable.Map$;
import scala.collection.immutable.Set$;
import scala.collection.immutable.Seq;
import scala.collection.mutable.ArrayBuffer;
import java.util.HashSet;
import java.util.Optional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

/** A read-only filesystem snapshot backed by server resources. */
public final class ResourceManagerFileSystem extends JavaInputStreamFileSystem {
    private final java.util.Map<String, byte[]> files;
    private final java.util.Set<String> directories;

    public ResourceManagerFileSystem(scala.collection.immutable.Map<String, byte[]> files,
                                     scala.collection.immutable.Set<String> directories) {
        this(scala.jdk.javaapi.CollectionConverters.asJava(files),
                scala.jdk.javaapi.CollectionConverters.asJava(directories));
    }

    private ResourceManagerFileSystem(java.util.Map<String, byte[]> files,
                                      java.util.Set<String> directories) {
        this.files = files;
        this.directories = directories;
    }

    @Override
    public long spaceTotal() {
        return spaceUsed();
    }

    @Override
    public long spaceUsed() {
        long total = 0;
        for (byte[] bytes : files.values()) total += bytes.length;
        return total;
    }

    @Override
    public boolean exists(String path) {
        String clean = clean(path);
        return files.containsKey(clean) || directories.contains(clean);
    }

    @Override
    public long size(String path) {
        byte[] bytes = files.get(clean(path));
        return bytes == null ? 0L : bytes.length;
    }

    @Override
    public boolean isDirectory(String path) {
        return directories.contains(clean(path));
    }

    @Override
    public long lastModified(String path) {
        return 0L;
    }

    @Override
    public String[] list(String path) {
        String clean = clean(path);
        if (!exists(clean)) throw ResourceManagerFileSystem.<RuntimeException>sneaky(new FileNotFoundException("no such file or directory: " + path));
        if (!isDirectory(clean)) return null;

        String prefix = clean.isEmpty() ? "" : clean + "/";
        LinkedHashSet<String> children = new LinkedHashSet<>();
        for (String entry : files.keySet()) addChild(entry, clean, prefix, children);
        for (String entry : directories) addChild(entry, clean, prefix, children);
        return children.toArray(new String[0]);
    }

    private static void addChild(String entry, String clean, String prefix, LinkedHashSet<String> children) {
        if (entry.startsWith(prefix) && !entry.equals(clean)) {
            String relative = entry.substring(prefix.length());
            int slash = relative.indexOf('/');
            children.add(slash < 0 ? relative : relative.substring(0, slash) + "/");
        }
    }

    @Override
    public Option<InputChannel> openInputChannel(String path) {
        byte[] bytes = files.get(clean(path));
        return bytes == null ? Option$.MODULE$.empty()
                : Option$.MODULE$.apply(new InputStreamChannel(new ByteArrayInputStream(bytes)));
    }

    public static Option<byte[]> readResource(ResourceManager manager, ResourceLocation location) {
        Optional<byte[]> bytes = readResourceOptional(manager, location);
        return bytes.<Option<byte[]>>map(Option$.MODULE$::apply).orElseGet(Option$.MODULE$::empty);
    }

    public static Optional<byte[]> readResourceOptional(ResourceManager manager, ResourceLocation location) {
        java.util.Optional<Resource> resource = manager.getResource(location);
        if (resource.isEmpty()) return Optional.empty();
        try {
            return Optional.of(read(resource.get().open()));
        }
        catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    public static ResourceManagerFileSystem fromResource(ResourceManager manager, ResourceLocation root) {
        String directory = stripSuffix(root.getPath(), "/");
        String prefix = directory + "/";
        java.util.Map<ResourceLocation, Resource> resources = manager.listResources(directory,
                location -> location.getNamespace().equals(root.getNamespace())
                        && location.getPath().startsWith(prefix));
        if (resources.isEmpty()) return null;

        java.util.Map<String, byte[]> files = new java.util.HashMap<>();
        for (Entry<ResourceLocation, Resource> resource : resources.entrySet()) {
            String relative = resource.getKey().getPath().substring(prefix.length());
            try {
                files.put(relative, read(resource.getValue().open()));
            }
            catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }

        java.util.Set<String> directories = new HashSet<String>();
        directories.add("");
        for (String path : files.keySet()) {
            String[] parts = path.split("/");
            StringBuilder parent = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) parent.append('/');
                parent.append(parts[i]);
                directories.add(parent.toString());
            }
        }
        return new ResourceManagerFileSystem(files, directories);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable error) throws T {
        throw (T) error;
    }

    private static byte[] read(InputStream input) throws IOException {
        try (InputStream in = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) >= 0) {
                if (count > 0) output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static String clean(String path) {
        return stripSuffix(stripPrefix(path.replace("\\", "/"), "/"), "/");
    }

    private static String stripPrefix(String value, String prefix) {
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static String stripSuffix(String value, String suffix) {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }
}
