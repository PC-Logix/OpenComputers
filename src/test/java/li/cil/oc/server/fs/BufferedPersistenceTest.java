package li.cil.oc.server.fs;

import li.cil.oc.util.SafeThreadPool;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.nio.file.Files;
import scala.Option;
import scala.Option$;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class BufferedPersistenceTest {
    @Test
    public void waitingIgnoresCompletedAndCancelledSavesAndReportsTimeout() {
        BufferedPersistence persistence = new BufferedPersistence();
        List<String> warnings = new ArrayList<>();
        persistence.setSaving(CompletableFuture.completedFuture(null));
        persistence.waitForSaving(1, TimeUnit.SECONDS, warnings::add);
        assertTrue(warnings.isEmpty());

        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        cancelled.cancel(false);
        persistence.setSaving(cancelled);
        persistence.waitForSaving(1, TimeUnit.SECONDS, warnings::add);
        assertTrue(warnings.isEmpty());

        persistence.setSaving(new CompletableFuture<>());
        persistence.waitForSaving(1, TimeUnit.MILLISECONDS, warnings::add);
        assertEquals(List.of("Waiting for filesystem to save took two minutes! Aborting."), warnings);
    }

    @Test
    public void scheduleSaveStoresTheFutureAndRunsTheSaveAction() throws Exception {
        SafeThreadPool savePool = new SafeThreadPool("BufferedPersistenceTest", 1);
        savePool.newThreadPool();
        try {
            BufferedPersistence persistence = new BufferedPersistence();
            CountDownLatch ran = new CountDownLatch(1);
            persistence.scheduleSave(savePool, ran::countDown);

            assertNull(persistence.saving().get(5, TimeUnit.SECONDS));
            assertTrue(ran.await(1, TimeUnit.SECONDS));
        } finally {
            savePool.waitForCompletion();
        }
    }

    @Test
    public void successfulDeleteAndRenameRecordOnlyTheirSourceForDiskCleanup() throws Exception {
        File root = Files.createTempDirectory("oc-buffered-deletions").toFile();
        try {
            File deleted = new File(root, "deleted.bin");
            File renamed = new File(root, "renamed.bin");
            File kept = new File(root, "kept.bin");
            for (File file : List.of(deleted, renamed, kept)) {
                Files.write(file.toPath(), new byte[]{1});
                file.setLastModified(1_000L);
            }
            TestBuffered filesystem = new TestBuffered(root);
            BufferedPersistence persistence = filesystem.persistence();

            assertTrue(persistence.delete("deleted.bin", () -> true));
            assertFalse(persistence.delete("kept.bin", () -> false));
            assertTrue(persistence.rename("renamed.bin", "destination.bin", () -> true));
            persistence.saveFiles(filesystem, (path, file, modified) -> false);

            assertFalse(deleted.exists());
            assertFalse(renamed.exists());
            assertTrue(kept.exists());
        } finally {
            File[] files = root.listFiles();
            if (files != null) for (File file : files) Files.deleteIfExists(file.toPath());
            Files.deleteIfExists(root.toPath());
        }
    }

    private static final class TestBuffered extends JavaOutputStreamFileSystem implements Buffered {
        private final File root;
        private final BufferedPersistence persistence = new BufferedPersistence();
        private TestBuffered(File root) { this.root = root; }
        @Override public File fileRoot() { return root; }
        @Override public BufferedPersistence persistence() { return persistence; }
        @Override public long spaceTotal() { return 0; }
        @Override public long spaceUsed() { return 0; }
        @Override public boolean exists(String path) { return false; }
        @Override public long size(String path) { return 0; }
        @Override public boolean isDirectory(String path) { return false; }
        @Override public long lastModified(String path) { return 0; }
        @Override public String[] list(String path) { return new String[0]; }
        @Override public Option<InputChannel> openInputChannel(String path) { return Option$.MODULE$.empty(); }
        @Override public Option<OutputHandle> openOutputHandle(int id, String path, li.cil.oc.api.fs.Mode mode) {
            return Option$.MODULE$.empty();
        }
    }
}
