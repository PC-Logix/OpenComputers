package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import org.junit.Test;
import scala.Option;
import scala.Option$;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import scala.Option;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class FileBackedHandleTest {
    @Test
    public void inputChannelReadsAndSeeksUsingTheFileChannel() throws Exception {
        File file = File.createTempFile("oc-file-input", ".bin");
        file.deleteOnExit();
        Files.write(file.toPath(), new byte[]{2, 4, 6, 8});
        FileInputChannel input = new FileInputChannel(file);
        byte[] first = new byte[2];
        assertEquals(2, input.read(first));
        assertArrayEquals(new byte[]{2, 4}, first);
        assertEquals(2, input.position());
        assertEquals(3, input.position(3));
        byte[] last = new byte[1];
        assertEquals(1, input.read(last));
        assertArrayEquals(new byte[]{8}, last);
        input.close();
        assertFalse(input.isOpen());
    }

    @Test
    public void randomAccessOutputHandlePreservesReadWriteAndCloseBehavior() throws Exception {
        File file = File.createTempFile("oc-file-output", ".bin");
        try {
            TestOwner owner = new TestOwner();
            RandomAccessFile randomAccess = new RandomAccessFile(file, "rw");
            RandomAccessOutputHandle output = new RandomAccessOutputHandle(owner, 9, "file", Mode.ReadWrite, randomAccess);
            output.write(new byte[]{1, 2, 3});
            output.seek(1);
            output.write(new byte[]{7});
            assertEquals(3, output.length());
            assertEquals(2, output.position());
            output.close();
            assertFalse(randomAccess.getChannel().isOpen());
            assertArrayEquals(new byte[]{1, 7, 3}, Files.readAllBytes(file.toPath()));
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    public void readOnlyResourceFilesystemExposesDirectoryContentsAndInputChannels() throws Exception {
        Path directory = Files.createTempDirectory("oc-read-only-resource");
        try {
            byte[] contents = new byte[]{10, 20, 30};
            Files.write(directory.resolve("disk.bin"), contents);
            ReadOnlyFileSystem filesystem = new ReadOnlyFileSystem(directory.toFile());

            assertTrue(filesystem.exists("disk.bin"));
            assertEquals(contents.length, filesystem.size("disk.bin"));
            assertEquals(contents.length, filesystem.spaceTotal());
            assertArrayEquals(new String[]{"disk.bin"}, filesystem.list(""));

            Option<InputChannel> opened = filesystem.openInputChannel("disk.bin");
            assertTrue(opened.isDefined());
            InputChannel input = opened.get();
            byte[] actual = new byte[contents.length];
            assertEquals(contents.length, input.read(actual));
            assertArrayEquals(contents, actual);
            input.close();
        } finally {
            Files.deleteIfExists(directory.resolve("disk.bin"));
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void fileOutputFactoryCreatesHandlesAndRunsSaveBeforeUpdatingTheRoot() throws Exception {
        Path directory = Files.createTempDirectory("oc-file-output-factory");
        try {
            TestOwner owner = new TestOwner();
            Option<OutputHandle> opened = FileOutputFileSystemJava.openOutputHandle(
                    owner, directory.toFile(), 13, "disk.bin", Mode.Write);
            assertTrue(opened.isDefined());
            opened.get().write(new byte[]{4, 2});
            opened.get().close();
            assertArrayEquals(new byte[]{4, 2}, Files.readAllBytes(directory.resolve("disk.bin")));

            AtomicBoolean saved = new AtomicBoolean();
            FileOutputFileSystemJava.saveData(directory.toFile(), () -> {
                saved.set(true);
                directory.toFile().setLastModified(1_000L);
            });
            assertTrue(saved.get());
            assertTrue(directory.toFile().lastModified() > 1_000L);
        } finally {
            Files.deleteIfExists(directory.resolve("disk.bin"));
            Files.deleteIfExists(directory);
        }
    }

    private static final class TestOwner extends JavaOutputStreamFileSystem {
        @Override public long spaceTotal() { return 0; }
        @Override public long spaceUsed() { return 0; }
        @Override public boolean exists(String path) { return false; }
        @Override public long size(String path) { return 0; }
        @Override public boolean isDirectory(String path) { return false; }
        @Override public long lastModified(String path) { return 0; }
        @Override public String[] list(String path) { return new String[0]; }
        @Override public Option<InputChannel> openInputChannel(String path) { return Option$.MODULE$.empty(); }
        @Override public Option<OutputHandle> openOutputHandle(int id, String path, Mode mode) { return Option$.MODULE$.empty(); }
    }
}
