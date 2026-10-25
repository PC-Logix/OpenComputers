package li.cil.oc.server.fs;

import org.apache.commons.io.FileUtils;
import li.cil.oc.util.SafeThreadPool;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Disk mirror bookkeeping and recursive file walking for buffered filesystems. */
public final class BufferedPersistence {
    @FunctionalInterface public interface RestoreFile { boolean restore(String path, File file); }
    @FunctionalInterface public interface StoreFile { boolean store(String path, File file, long modified); }

    private final Map<String, Long> deletions = new HashMap<>();
    private Future<?> saving;

    public synchronized void recordDeletion(String path, long time) { deletions.put(path, time); }
    public synchronized Future<?> saving() { return saving; }
    public synchronized void setSaving(Future<?> value) { saving = value; }

    public boolean delete(String path, BooleanSupplier fallback) {
        if (!fallback.getAsBoolean()) return false;
        recordDeletion(path, System.currentTimeMillis());
        return true;
    }

    public boolean rename(String from, String to, BooleanSupplier fallback) {
        if (!fallback.getAsBoolean()) return false;
        recordDeletion(from, System.currentTimeMillis());
        return true;
    }

    public void waitForSaving(Consumer<String> warning) {
        waitForSaving(120L, TimeUnit.SECONDS, warning);
    }

    void waitForSaving(long timeout, TimeUnit unit, Consumer<String> warning) {
        Future<?> future = saving();
        if (future == null) return;
        try {
            future.get(timeout, unit);
        } catch (TimeoutException ignored) {
            warning.accept("Waiting for filesystem to save took two minutes! Aborting.");
        } catch (CancellationException ignored) {
            // A cancelled save has no remaining work to wait for.
        } catch (InterruptedException | ExecutionException error) {
            throw sneaky(error);
        }
    }

    public void scheduleSave(SafeThreadPool saveHandler, Runnable saveFiles) {
        Optional<Future<?>> future = saveHandler.withPoolJava(pool -> pool.submit(saveFiles));
        future.ifPresent(this::setSaving);
    }

    public void loadFiles(Buffered filesystem, RestoreFile restore) {
        synchronized (filesystem) {
            File root = filesystem.fileRoot();
            String[] contents = root.list();
            if (contents == null || contents.length == 0) root.delete();
            else loadDirectory(filesystem, "", root, restore);
        }
    }

    private void loadDirectory(Buffered filesystem, String path, File directory, RestoreFile restore) {
        filesystem.makeDirectory(path);
        File[] children = directory.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!FileSystemPathJava.isValidFilename(child.getName())) continue;
                String childPath = path + child.getName();
                if (child.isDirectory() && child.list() != null) {
                    loadDirectory(filesystem, childPath + "/", child, restore);
                } else if (!filesystem.exists(childPath) || !filesystem.isDirectory(childPath)) {
                    if (restore.restore(childPath, child))
                        filesystem.setLastModified(childPath, child.lastModified());
                }
            }
        }
        filesystem.setLastModified(path, directory.lastModified());
    }

    public void saveFiles(Buffered filesystem, StoreFile store) {
        synchronized (filesystem) {
            for (Map.Entry<String, Long> deletion : drainDeletions().entrySet()) {
                File file = new File(filesystem.fileRoot(), deletion.getKey());
                if (FileUtils.isFileOlder(file, deletion.getValue())) FileUtils.deleteQuietly(file);
            }

            String[] rootContents = filesystem.list("");
            if (rootContents == null || rootContents.length == 0) {
                filesystem.fileRoot().delete();
            } else {
                saveDirectory(filesystem, "", store);
            }
        }
    }

    private synchronized Map<String, Long> drainDeletions() {
        Map<String, Long> pending = new HashMap<>(deletions);
        deletions.clear();
        return pending;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable error) throws T {
        throw (T) error;
    }

    private boolean saveDirectory(Buffered filesystem, String path, StoreFile store) {
        File directory = new File(filesystem.fileRoot(), path);
        directory.mkdirs();
        boolean changed = false;
        String[] children = filesystem.list(path);
        for (String child : children) {
            String childPath = path + child;
            if (filesystem.isDirectory(childPath)) {
                changed = saveDirectory(filesystem, childPath, store) || changed;
            } else {
                File file = new File(filesystem.fileRoot(), childPath);
                long modified = filesystem.lastModified(childPath);
                if (modified == 0 || !file.exists() || FileUtils.isFileOlder(file, modified))
                    changed = store.store(childPath, file, modified) || changed;
            }
        }
        if (changed) directory.setLastModified(filesystem.lastModified(path));
        return changed;
    }
}
