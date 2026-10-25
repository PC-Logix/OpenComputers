package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public final class FileSystemCompositionTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void ramRestoresTreeAndOpenHandleBeforeAccountingAndClearsOnClose() throws Exception {
        RamFileSystem original = new RamFileSystem(7, 1);
        assertTrue(original.makeDirectory("folder"));
        int id = original.open("folder/data", Mode.ReadWriteTruncate);
        Handle output = original.getHandle(id);
        output.write(new byte[]{3, 5, 8});
        assertEquals(6, original.spaceUsed());
        try {
            output.write(new byte[]{13, 21});
            fail("capacity should reject the whole write");
        } catch (IOException expected) {
            assertEquals(3, output.length());
            assertEquals(6, original.spaceUsed());
        }
        output.seek(1);
        CompoundTag saved = new CompoundTag();
        original.saveData(saved);
        original.close();
        assertFalse(original.exists("folder/data"));
        assertNull(original.getHandle(id));
        assertEquals(1, original.spaceUsed());

        RamFileSystem restored = new RamFileSystem(7, 1);
        try {
            restored.loadData(saved);
            assertEquals(6, restored.spaceUsed());
            Handle handle = restored.getHandle(id);
            assertNotNull(handle);
            assertEquals(1, handle.position());
            byte[] bytes = new byte[2];
            assertEquals(2, handle.read(bytes));
            assertArrayEquals(new byte[]{5, 8}, bytes);
        } finally {
            restored.close();
        }
    }

    @Test
    public void diskRestoresOutputPositionAndUpdatesAccountingAfterRenameAndDelete() throws Exception {
        File root = temporary.newFolder("disk");
        ReadWriteFileSystem original = new ReadWriteFileSystem(root, 100, 1);
        assertTrue(original.makeDirectory("folder"));
        int id = original.open("folder/data", Mode.ReadWriteTruncate);
        Handle handle = original.getHandle(id);
        handle.write(new byte[]{1, 2, 3, 4});
        handle.seek(2);
        CompoundTag saved = new CompoundTag();
        original.saveData(saved);
        original.close();

        ReadWriteFileSystem restored = new ReadWriteFileSystem(root, 100, 1);
        try {
            restored.loadData(saved);
            assertEquals(7, restored.spaceUsed());
            Handle reopened = restored.getHandle(id);
            assertNotNull(reopened);
            assertEquals(2, reopened.position());
            assertEquals(Mode.ReadWriteTruncate, ((OutputHandle) reopened).mode());
            byte[] bytes = new byte[2];
            assertEquals(2, reopened.read(bytes));
            assertArrayEquals(new byte[]{3, 4}, bytes);
            reopened.close();
            assertTrue(restored.rename("folder/data", "folder/renamed"));
            assertEquals(7, restored.spaceUsed());
            assertTrue(restored.delete("folder/renamed"));
            assertEquals(2, restored.spaceUsed());
            assertTrue(restored.delete("folder"));
            assertEquals(1, restored.spaceUsed());
            assertTrue(FileSystem.INSTANCE.asReadOnly(restored).isReadOnly());
        } finally {
            restored.close();
        }
    }

    @Test
    public void bufferedLoadsBeyondCapacityAndSavesRenamesThroughTheMirrorExecutor() throws Exception {
        File root = temporary.newFolder("buffered");
        File folder = new File(root, "folder");
        assertTrue(folder.mkdir());
        File data = new File(folder, "data");
        Files.write(data.toPath(), new byte[]{7, 11, 13});
        assertTrue(data.setLastModified(1000));

        BufferedFileSystem filesystem = new BufferedFileSystem(root, 1, 1, () -> false);
        try {
            filesystem.loadData(new CompoundTag());
            assertEquals(6, filesystem.spaceUsed());
            assertFalse(filesystem.accounting().ignoringCapacity());
            int id = filesystem.open("folder/data", Mode.Read);
            byte[] bytes = new byte[3];
            assertEquals(3, filesystem.getHandle(id).read(bytes));
            assertArrayEquals(new byte[]{7, 11, 13}, bytes);
            filesystem.getHandle(id).close();

            assertTrue(filesystem.rename("folder/data", "folder/renamed"));
            CompoundTag saved = new CompoundTag();
            filesystem.saveData(saved);
            filesystem.persistence().saving().get(5, TimeUnit.SECONDS);
            assertFalse(saved.contains("root"));
            assertFalse(data.exists());
            assertArrayEquals(new byte[]{7, 11, 13}, Files.readAllBytes(new File(folder, "renamed").toPath()));
        } finally {
            filesystem.close();
            Buffered.fileSaveHandler().waitForCompletion();
        }
    }
}
