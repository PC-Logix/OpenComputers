package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import scala.Option$;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class BufferedFileOperationsTest {
    @Test
    public void restoreCopiesTheDiskContentsAndClosesTheOutputHandle() throws Exception {
        File source = File.createTempFile("oc-buffered-restore", ".bin");
        try {
            byte[] contents = new byte[]{1, 2, 3, 4};
            Files.write(source.toPath(), contents);
            TestHandle output = new TestHandle();

            assertTrue(BufferedFileOperations.restore(source, () -> Option$.MODULE$.apply(output)));

            assertArrayEquals(contents, output.bytes.toByteArray());
            assertTrue(output.isClosed());
            assertEquals(1, output.owner.removals.get());
        } finally {
            Files.deleteIfExists(source.toPath());
        }
    }

    @Test
    public void storeCopiesChannelContentsAndRestoresModificationTime() throws Exception {
        File destination = File.createTempFile("oc-buffered-store", ".bin");
        try {
            byte[] contents = new byte[20 * 1024];
            for (int index = 0; index < contents.length; index++) contents[index] = (byte) (index * 31);
            long modified = 1_600_000_000_000L;
            TrackingChannel input = new TrackingChannel(contents);

            assertTrue(BufferedFileOperations.store(destination, modified,
                    () -> Option$.MODULE$.apply(input)));

            assertArrayEquals(contents, Files.readAllBytes(destination.toPath()));
            assertEquals(modified, destination.lastModified());
            assertTrue(input.closed);
        } finally {
            Files.deleteIfExists(destination.toPath());
        }
    }

    private static final class TestOwner implements OutputHandleOwner {
        private final AtomicInteger removals = new AtomicInteger();
        @Override public void removeOutputHandle(int handle) { removals.incrementAndGet(); }
    }

    private static final class TestHandle extends OutputHandle {
        private final TestOwner owner;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private TestHandle() { this(new TestOwner()); }
        private TestHandle(TestOwner owner) {
            super(owner, 1, "restore", Mode.Write);
            this.owner = owner;
        }
        @Override public long length() { return bytes.size(); }
        @Override public long position() { return bytes.size(); }
        @Override public void write(byte[] value) { bytes.write(value, 0, value.length); }
    }

    private static final class TrackingChannel implements InputChannel {
        private final byte[] contents;
        private int position;
        private boolean closed;
        private TrackingChannel(byte[] contents) { this.contents = contents; }
        @Override public boolean isOpen() { return !closed; }
        @Override public void close() { closed = true; }
        @Override public long position() { return position; }
        @Override public long position(long newPosition) { position = Math.toIntExact(newPosition); return position; }
        @Override public int read(byte[] destination) {
            if (position == contents.length) return -1;
            int count = Math.min(destination.length, contents.length - position);
            System.arraycopy(contents, position, destination, 0, count);
            position += count;
            return count;
        }
    }
}
