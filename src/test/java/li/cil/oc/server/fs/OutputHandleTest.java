package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class OutputHandleTest {
    @Test
    public void closeRemovesTheHandleOnceAndModeCanBeRestored() {
        AtomicInteger removals = new AtomicInteger();
        TestOwner owner = new TestOwner(removals);
        TestHandle handle = new TestHandle(owner, 17, "disk/file", Mode.Write);

        assertEquals(17, handle.handle());
        assertEquals("disk/file", handle.path());
        assertEquals(Mode.Write, handle.mode());
        handle.restoreMode(Mode.Append);
        assertEquals(Mode.Append, handle.mode());
        assertFalse(handle.isClosed());

        handle.close();
        handle.close();

        assertTrue(handle.isClosed());
        assertEquals(1, removals.get());
    }

    private static final class TestOwner implements OutputHandleOwner {
        private final AtomicInteger removals;
        private TestOwner(AtomicInteger removals) { this.removals = removals; }
        @Override public void removeOutputHandle(int handle) { removals.incrementAndGet(); }
    }

    private static final class TestHandle extends OutputHandle {
        private TestHandle(OutputHandleOwner owner, int handle, String path, Mode mode) {
            super(owner, handle, path, mode);
        }

        @Override public long length() { return 0; }
        @Override public long position() { return 0; }
        @Override public void write(byte[] value) { }
    }
}
