package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import scala.Option;
import scala.Option$;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.mutable.LinkedHashMap;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Callable;

public class CompositeReadOnlyFileSystem implements li.cil.oc.api.fs.FileSystem {
    public LinkedHashMap<String, li.cil.oc.api.fs.FileSystem> parts = new LinkedHashMap<>();

    public CompositeReadOnlyFileSystem(
            LinkedHashMap<String, Callable<li.cil.oc.api.fs.FileSystem>> factories) {
        Iterator<Tuple2<String, Callable<li.cil.oc.api.fs.FileSystem>>> iterator = factories.iterator();
        while (iterator.hasNext()) {
            Tuple2<String, Callable<li.cil.oc.api.fs.FileSystem>> entry = iterator.next();
            try {
                li.cil.oc.api.fs.FileSystem fileSystem = entry._2().call();
                if (fileSystem != null) parts.put(entry._1(), fileSystem);
            }
            catch (Exception error) {
                throw CompositeReadOnlyFileSystem.<RuntimeException>sneaky(error);
            }
        }
    }

    @Override public boolean isReadOnly() { return true; }

    @Override
    public long spaceTotal() {
        long total = 0;
        for (li.cil.oc.api.fs.FileSystem fileSystem : scala.jdk.javaapi.CollectionConverters.asJava(parts.values())) total += fileSystem.spaceTotal();
        return Math.max(spaceUsed(), total);
    }

    @Override
    public long spaceUsed() {
        long total = 0;
        for (li.cil.oc.api.fs.FileSystem fileSystem : scala.jdk.javaapi.CollectionConverters.asJava(parts.values())) total += fileSystem.spaceUsed();
        return total;
    }

    @Override public boolean exists(String path) { return findFileSystem(path).isDefined(); }

    @Override
    public long size(String path) {
        Option<li.cil.oc.api.fs.FileSystem> fileSystem = findFileSystem(path);
        return fileSystem.isDefined() ? fileSystem.get().size(path) : 0L;
    }

    @Override
    public boolean isDirectory(String path) {
        Option<li.cil.oc.api.fs.FileSystem> fileSystem = findFileSystem(path);
        return fileSystem.isDefined() && fileSystem.get().isDirectory(path);
    }

    @Override
    public long lastModified(String path) {
        Option<li.cil.oc.api.fs.FileSystem> fileSystem = findFileSystem(path);
        return fileSystem.isDefined() ? fileSystem.get().lastModified(path) : 0L;
    }

    @Override
    public String[] list(String path) {
        if (!isDirectory(path)) return null;
        LinkedHashSet<String> entries = new LinkedHashSet<>();
        for (li.cil.oc.api.fs.FileSystem fileSystem : scala.jdk.javaapi.CollectionConverters.asJava(parts.values())) {
            if (!fileSystem.exists(path)) continue;
            try {
                String[] listed = fileSystem.list(path);
                if (listed == null) continue;
                for (String entry : listed) {
                    String file = entry.endsWith("/") ? entry.substring(0, entry.length() - 1) : entry;
                    String directory = file + "/";
                    entries.remove(file);
                    entries.remove(directory);
                    entries.add(entry);
                }
            }
            catch (Throwable ignored) {
            }
        }
        return entries.toArray(new String[0]);
    }

    @Override public boolean delete(String path) { return false; }
    @Override public boolean makeDirectory(String path) { return false; }
    @Override public boolean rename(String from, String to) { return false; }
    @Override public boolean setLastModified(String path, long time) { return false; }

    @Override
    public int open(String path, Mode mode) throws FileNotFoundException {
        Option<li.cil.oc.api.fs.FileSystem> fileSystem = findFileSystem(path);
        if (fileSystem.isDefined()) return fileSystem.get().open(path, mode);
        throw new FileNotFoundException(path);
    }

    @Override
    public Handle getHandle(int handle) {
        for (li.cil.oc.api.fs.FileSystem fileSystem : scala.jdk.javaapi.CollectionConverters.asJava(parts.values())) {
            Handle value = fileSystem.getHandle(handle);
            if (value != null) return value;
        }
        return null;
    }

    @Override
    public void close() {
        for (li.cil.oc.api.fs.FileSystem fileSystem : scala.jdk.javaapi.CollectionConverters.asJava(parts.values())) fileSystem.close();
    }

    @Override
    public void loadData(CompoundTag nbt) {
        for (Tuple2<String, li.cil.oc.api.fs.FileSystem> entry : scala.jdk.javaapi.CollectionConverters.asJava(parts).entrySet()
                .stream().map(value -> new Tuple2<>(value.getKey(), value.getValue())).toList()) {
            entry._2().loadData(nbt.getCompound(entry._1()));
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        for (Tuple2<String, li.cil.oc.api.fs.FileSystem> entry : scala.jdk.javaapi.CollectionConverters.asJava(parts).entrySet()
                .stream().map(value -> new Tuple2<>(value.getKey(), value.getValue())).toList()) {
            CompoundTag child = nbt.getCompound(entry._1());
            entry._2().saveData(child);
            nbt.put(entry._1(), child);
        }
    }

    protected Option<li.cil.oc.api.fs.FileSystem> findFileSystem(String path) {
        List<li.cil.oc.api.fs.FileSystem> values = new ArrayList<>();
        for (li.cil.oc.api.fs.FileSystem fileSystem : scala.jdk.javaapi.CollectionConverters.asJava(parts.values())) values.add(fileSystem);
        for (int i = values.size() - 1; i >= 0; i--) {
            li.cil.oc.api.fs.FileSystem fileSystem = values.get(i);
            if (fileSystem.exists(path)) return Option$.MODULE$.apply(fileSystem);
        }
        return Option$.MODULE$.empty();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable error) throws T {
        throw (T) error;
    }
}
