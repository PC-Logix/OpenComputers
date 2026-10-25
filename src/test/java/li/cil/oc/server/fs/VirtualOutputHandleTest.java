package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import org.junit.Test;
import scala.Option;
import scala.Option$;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class VirtualOutputHandleTest {
    @Test
    public void writeSeekReadAndCloseOperateOnTheVirtualFile() throws Exception {
        TestOwner owner = new TestOwner();
        VirtualFileSystem.VirtualFile file = new VirtualFileSystem.VirtualFile();
        VirtualOutputHandle handle = new VirtualOutputHandle(file, owner, 3, "file", Mode.ReadWrite);
        file.setHandle(handle);

        handle.write(new byte[]{1, 2, 3});
        assertEquals(3, handle.position());
        handle.seek(1);
        handle.write(new byte[]{9});
        handle.seek(0);
        byte[] contents = new byte[3];
        assertEquals(3, handle.read(contents));
        assertArrayEquals(new byte[]{1, 9, 3}, contents);

        handle.close();
        assertNull(file.handle());
        assertTrue(handle.isClosed());
    }

    @Test
    public void appendStartsAtEndAndNegativeSeekIsRejected() throws Exception {
        TestOwner owner = new TestOwner();
        VirtualFileSystem.VirtualFile file = new VirtualFileSystem.VirtualFile();
        file.data().add((byte) 4);
        VirtualOutputHandle handle = new VirtualOutputHandle(file, owner, 4, "file", Mode.Append);
        file.setHandle(handle);

        assertEquals(1, handle.position());
        handle.write(new byte[]{5});
        assertEquals(2, handle.position());
        try {
            handle.seek(-1);
        } catch (IOException expected) {
            handle.close();
            return;
        }
        throw new AssertionError("negative seek should fail");
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
