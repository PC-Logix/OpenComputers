package li.cil.oc.server.fs;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import li.cil.oc.OpenComputers;
import li.cil.oc.server.fs.InputChannel;
import li.cil.oc.server.fs.InputStreamChannel;
import net.minecraft.util.Tuple;
import scala.Option;
import scala.Option$;
import scala.collection.Iterable;
import scala.jdk.javaapi.CollectionConverters;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileInputStreamFileSystem extends JavaInputStreamFileSystem {
    private static final Cache<String, ArchiveDirectory> CACHE =
            CacheBuilder.newBuilder().weakValues().build();

    private final ArchiveDirectory archive;
    private volatile Long cachedSpaceUsed;

    public ZipFileInputStreamFileSystem(ArchiveDirectory archive) {
        this.archive = archive;
    }

    public long spaceTotal() {
        return spaceUsed();
    }

    public long spaceUsed() {
        Long cached = cachedSpaceUsed;
        if (cached != null) return cached;
        synchronized (ZipFileInputStreamFileSystem.class) {
            if (cachedSpaceUsed == null) cachedSpaceUsed = totalSize(archive);
            return cachedSpaceUsed;
        }
    }

    private static long totalSize(ArchiveDirectory directory) {
        long total = 0;
        for (Archive child : directory.children) {
            total += child.isDirectory ? totalSize((ArchiveDirectory) child) : child.size();
        }
        return total;
    }

    @Override
    public boolean exists(String path) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            return entry(path).isDefined();
        }
    }

    @Override
    public long size(String path) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            Option<Archive> value = entry(path);
            if (value.isDefined() && !value.get().isDirectory) return value.get().size();
            return 0L;
        }
    }

    @Override
    public boolean isDirectory(String path) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            return entry(path).isDefined() && entry(path).get().isDirectory;
        }
    }

    @Override
    public long lastModified(String path) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            Option<Archive> value = entry(path);
            return value.isDefined() ? value.get().lastModified : 0L;
        }
    }

    @Override
    public String[] list(String path) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            Option<Archive> value = entry(path);
            return value.isDefined() && value.get().isDirectory ? value.get().list() : null;
        }
    }

    @Override
    public Option<InputChannel> openInputChannel(String path) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            Option<Archive> value = entry(path);
            return value.map(item -> new InputStreamChannel(item.openStream()));
        }
    }

    private Option<Archive> entry(String path) {
        String cleanPath = "/" + path.replace("\\", "/").replace("//", "/")
                .replaceAll("^/", "").replaceAll("/$", "");
        if (cleanPath.equals("/")) return Option$.MODULE$.apply(archive);
        return archive.find(CollectionConverters.asScala(List.of(cleanPath.split("/"))));
    }

    public static ZipFileInputStreamFileSystem fromFile(File file, String innerPath) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            System.out.println("zip loading");
            try {
                ArchiveDirectory root = CACHE.get(file.getPath() + ":" + innerPath, new Callable<>() {
                    @Override
                    public ArchiveDirectory call() throws Exception {
                        ZipFile zip = new ZipFile(file.getPath());
                        try {
                            String cleanedPath = stripPrefix(stripSuffix(innerPath, "/"), "/") + "/";
                            boolean hasRoot = false;
                            Set<ArchiveDirectory> directories = new HashSet<>();
                            Set<ArchiveFile> files = new HashSet<>();
                            var iterator = zip.entries();
                            while (iterator.hasMoreElements()) {
                                ZipEntry entry = iterator.nextElement();
                                if (entry.getName().startsWith(cleanedPath)) {
                                    hasRoot = true;
                                    if (entry.isDirectory()) directories.add(new ArchiveDirectory(entry, cleanedPath));
                                    else files.add(new ArchiveFile(zip, entry, cleanedPath));
                                }
                            }
                            if (!hasRoot) {
                                throw new IllegalArgumentException("Root path " + innerPath
                                        + " doesn't exist in ZIP file " + file.getName() + ".");
                            }

                            Map<String, ArchiveDirectory> directoriesByPath = new HashMap<>();
                            for (ArchiveDirectory directory : directories) {
                                directoriesByPath.put(directory.path, directory);
                            }
                            java.util.function.Function<String, ArchiveDirectory> getOrCreateDir = new java.util.function.Function<>() {
                                @Override
                                public ArchiveDirectory apply(String path) {
                                    return directoriesByPath.computeIfAbsent(path, key -> {
                                        String entryPath = cleanedPath + (key.isEmpty() ? "" : key + "/");
                                        ArchiveDirectory directory = new ArchiveDirectory(new ZipEntry(entryPath), cleanedPath);
                                        directories.add(directory);
                                        return directory;
                                    });
                                }
                            };

                            for (ArchiveFile entry : files) {
                                String parentPath = entry.path.substring(0, Math.max(entry.path.lastIndexOf('/'), 0));
                                getOrCreateDir.apply(parentPath).children.add(entry);
                            }
                            for (ArchiveDirectory directory : new ArrayList<>(directories)) {
                                if (!directory.path.isEmpty()) {
                                    String parentPath = directory.path.substring(0,
                                            Math.max(directory.path.lastIndexOf('/'), 0));
                                    ArchiveDirectory parent = getOrCreateDir.apply(parentPath);
                                    if (!parent.children.contains(directory)) parent.children.add(directory);
                                }
                            }
                            return getOrCreateDir.apply("");
                        }
                        finally {
                            zip.close();
                        }
                    }
                });
                return root == null ? null : new ZipFileInputStreamFileSystem(root);
            }
            catch (Throwable error) {
                OpenComputers.log().warn("Failed creating ZIP file system.", error);
                return null;
            }
        }
    }

    private static String stripPrefix(String value, String prefix) {
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static String stripSuffix(String value, String suffix) {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }

    public abstract static class Archive {
        public final String path;
        public final String name;
        public final long lastModified;
        public final boolean isDirectory;

        protected Archive(ZipEntry entry, String root) {
            path = stripSuffix(stripPrefix(entry.getName(), root), "/");
            name = path.substring(path.lastIndexOf('/') + 1);
            lastModified = entry.getTime();
            isDirectory = entry.isDirectory();
        }

        public abstract int size();

        public abstract String[] list();

        public abstract java.io.InputStream openStream();

        public abstract Option<Archive> find(Iterable<String> path);
    }

    private static final class ArchiveFile extends Archive {
        private final byte[] data;

        ArchiveFile(ZipFile zip, ZipEntry entry, String root) throws IOException {
            super(entry, root);
            try (java.io.InputStream input = zip.getInputStream(entry)) {
                data = input.readAllBytes();
            }
        }

        @Override public int size() { return data.length; }
        @Override public String[] list() { return null; }
        @Override public java.io.InputStream openStream() { return new ByteArrayInputStream(data); }

        @Override
        public Option<Archive> find(Iterable<String> path) {
            return path.size() == 1 && path.head().equals(name)
                    ? Option$.MODULE$.apply(this) : Option$.MODULE$.empty();
        }
    }

    private static final class ArchiveDirectory extends Archive {
        private final Set<Archive> children = new HashSet<>();

        ArchiveDirectory(ZipEntry entry, String root) {
            super(entry, root);
        }

        @Override public int size() { return 0; }

        @Override
        public String[] list() {
            return children.stream().map(child -> child.name + (child.isDirectory ? "/" : ""))
                    .toArray(String[]::new);
        }

        @Override public java.io.InputStream openStream() { return null; }

        @Override
        public Option<Archive> find(Iterable<String> path) {
            if (path.isEmpty() || !path.head().equals(name)) return Option$.MODULE$.empty();
            if (path.size() == 1) return Option$.MODULE$.apply(this);
            Iterable<String> subPath = path.drop(1);
            for (Archive child : children) {
                Option<Archive> found = child.find(subPath);
                if (found.isDefined()) return found;
            }
            return Option$.MODULE$.empty();
        }
    }
}
