package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import scala.Option;
import scala.Option$;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CapacityOperationsTest {
    @Test
    public void openWrapsTheHandleAndAccountsForSuccessfulWrites() throws Exception {
        TestFileSystem filesystem = new TestFileSystem();
        TestHandle inner = new TestHandle(filesystem);
        long initialUsage = filesystem.accounting().used();

        Option<OutputHandle> opened = CapacityOperations.open(filesystem, filesystem.accounting(), "/file", Mode.Write,
                () -> Option$.MODULE$.apply(inner));

        assertTrue(opened.isDefined());
        OutputHandle handle = opened.get();
        handle.write(new byte[]{1, 2, 3});
        assertEquals(initialUsage + 3, filesystem.accounting().used());
        assertEquals(3, handle.position());
        assertEquals(3, handle.length());
        handle.close();
        assertTrue(inner.isClosed());
    }

    @Test
    public void capacityWriteFailureDoesNotChangeTheWrappedHandle() throws Exception {
        TestFileSystem filesystem = new TestFileSystem();
        TestHandle inner = new TestHandle(filesystem);
        OutputHandle handle = CapacityOperations.open(filesystem, filesystem.accounting(), "/file", Mode.Write,
                () -> Option$.MODULE$.apply(inner)).get();
        filesystem.capacity = filesystem.accounting().used();

        try {
            handle.write(new byte[]{1});
        } catch (IOException expected) {
            assertEquals(0, inner.length());
            assertEquals(0, handle.position());
            return;
        }
        throw new AssertionError("write beyond available capacity should fail");
    }

    @Test
    public void loadTemporarilyIgnoresCapacityAndSavePersistsUsage() {
        TestFileSystem filesystem = new TestFileSystem();
        AtomicBoolean loadObservedIgnoringCapacity = new AtomicBoolean();
        CapacityOperations.load(filesystem.accounting(), () ->
                loadObservedIgnoringCapacity.set(filesystem.accounting().ignoringCapacity()));

        assertTrue(loadObservedIgnoringCapacity.get());
        assertFalse(filesystem.accounting().ignoringCapacity());

        CompoundTag saved = new CompoundTag();
        AtomicBoolean saveCalled = new AtomicBoolean();
        CapacityOperations.save(filesystem.accounting(), saved, () -> saveCalled.set(true));
        assertTrue(saveCalled.get());
        assertEquals(filesystem.accounting().used(), saved.getLong("capacity.used"));
    }

    @Test
    public void deleteAndDirectoryCreationAdjustUsageOnlyAfterTheFallbackSucceeds() {
        TestFileSystem filesystem = new TestFileSystem();
        long initialUsage = filesystem.accounting().used();
        assertFalse(CapacityOperations.delete(filesystem.accounting(), "/file", () -> false));
        assertEquals(initialUsage, filesystem.accounting().used());
        assertTrue(CapacityOperations.delete(filesystem.accounting(), "/file", () -> true));
        assertEquals(initialUsage - 1, filesystem.accounting().used());

        assertTrue(CapacityOperations.makeDirectory(filesystem.accounting(), () -> true));
        assertEquals(initialUsage, filesystem.accounting().used());
    }

    private static final class TestFileSystem extends JavaOutputStreamFileSystem implements Capacity {
        private final CapacityAccounting accounting = new CapacityAccounting(this, 1, 1);
        private long capacity = Long.MAX_VALUE;

        @Override public long capacity() { return capacity; }
        @Override public CapacityAccounting accounting() { return accounting; }
        @Override public long spaceTotal() { return capacity; }
        @Override public long spaceUsed() { return accounting.used(); }
        @Override public boolean exists(String path) { return path.equals("/file"); }
        @Override public long size(String path) { return 0; }
        @Override public boolean isDirectory(String path) { return false; }
        @Override public long lastModified(String path) { return 0; }
        @Override public String[] list(String path) { return new String[0]; }
        @Override public Option<InputChannel> openInputChannel(String path) { return Option$.MODULE$.empty(); }
        @Override public Option<OutputHandle> openOutputHandle(int id, String path, Mode mode) { return Option$.MODULE$.empty(); }
    }

    private static final class TestHandle extends OutputHandle {
        private final ByteArrayOutputStream contents = new ByteArrayOutputStream();
        private long position;

        private TestHandle(OutputStreamFileSystem owner) { super(owner, 7, "/file", Mode.Write); }
        @Override public long length() { return contents.size(); }
        @Override public long position() { return position; }
        @Override public long seek(long value) { position = value; return value; }
        @Override public void write(byte[] value) { contents.write(value, 0, value.length); position += value.length; }
    }
}
