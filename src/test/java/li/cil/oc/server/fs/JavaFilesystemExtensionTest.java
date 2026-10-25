package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Optional;

import static org.junit.Assert.*;

/** Compiled examples whose filesystem implementation hooks use only Java types. */
public final class JavaFilesystemExtensionTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void javaOptionalWritableHooksRetainDataModeAndPositionAcrossReload() throws Exception {
        File root = temporary.newFolder();
        Writable original = new Writable(root);
        int id = original.open("data", Mode.ReadWriteTruncate);
        Handle handle = original.getHandle(id);
        handle.write(new byte[]{2, 3, 5});
        handle.seek(1);
        CompoundTag saved = new CompoundTag();
        original.saveData(saved);
        original.close();

        Writable restored = new Writable(root);
        try {
            restored.loadData(saved);
            Handle reopened = restored.getHandle(id);
            assertNotNull(reopened);
            assertEquals(1, reopened.position());
            byte[] bytes = new byte[2];
            assertEquals(2, reopened.read(bytes));
            assertArrayEquals(new byte[]{3, 5}, bytes);
        } finally {
            restored.close();
        }

        ReadOnly readOnly = new ReadOnly(root);
        try {
            assertTrue(readOnly.isReadOnly());
            li.cil.oc.api.fs.FileSystem api = readOnly;
            try {
                api.open("data", Mode.Write);
                fail("read-only integrations must reject writable handles");
            } catch (FileNotFoundException expected) {
                assertEquals(3, readOnly.size("data"));
            }
            int input = readOnly.open("data", Mode.Read);
            byte[] bytes = new byte[3];
            assertEquals(3, readOnly.getHandle(input).read(bytes));
            assertArrayEquals(new byte[]{2, 3, 5}, bytes);
        } finally {
            readOnly.close();
        }
    }

    private static final class Writable extends JavaWritableStreamFileSystem implements FileOutputStreamFileSystem {
        private final File root;
        private Writable(File root) { this.root = root; }
        @Override public File root() { return root; }
        @Override protected Optional<InputChannel> openInputChannelJava(String path) throws IOException {
            return Optional.of(new FileInputChannel(file(path)));
        }
        @Override protected Optional<OutputHandle> openOutputHandleJava(int id, String path, Mode mode) throws IOException {
            return Optional.of(new RandomAccessOutputHandle(this, id, path, mode, new RandomAccessFile(file(path), "rw")));
        }
    }

    private static final class ReadOnly extends JavaReadOnlyStreamFileSystem implements FileInputStreamFileSystem {
        private final File root;
        private ReadOnly(File root) { this.root = root; }
        @Override public File root() { return root; }
        @Override protected Optional<InputChannel> openInputChannelJava(String path) throws IOException {
            return Optional.of(new FileInputChannel(file(path)));
        }
    }
}
