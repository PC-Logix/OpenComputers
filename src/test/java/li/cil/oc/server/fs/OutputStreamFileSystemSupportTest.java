package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import scala.Option;
import scala.Option$;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class OutputStreamFileSystemSupportTest {
    @Test
    public void outputHandlePositionAndModeSurviveSaveAndReload() throws Exception {
        MemoryFileSystem fileSystem = new MemoryFileSystem();
        int handleId = fileSystem.open("/file", Mode.Append);
        OutputHandle handle = (OutputHandle) fileSystem.getHandle(handleId);
        handle.seek(3);

        CompoundTag saved = new CompoundTag();
        fileSystem.saveData(saved);
        fileSystem.close();
        assertNull(fileSystem.getHandle(handleId));

        fileSystem.loadData(saved);
        OutputHandle restored = (OutputHandle) fileSystem.getHandle(handleId);
        assertEquals(3, restored.position());
        assertEquals(Mode.Append, restored.mode());

        restored.close();
        assertNull(fileSystem.getHandle(handleId));
    }

    private static final class MemoryFileSystem extends JavaOutputStreamFileSystem {
        private final byte[] contents = new byte[8];

        @Override public long spaceTotal() { return contents.length; }
        @Override public long spaceUsed() { return contents.length; }
        @Override public boolean exists(String path) { return path.equals("/file"); }
        @Override public long size(String path) { return exists(path) ? contents.length : 0; }
        @Override public boolean isDirectory(String path) { return false; }
        @Override public long lastModified(String path) { return 0; }
        @Override public String[] list(String path) { return null; }
        @Override public Option<InputChannel> openInputChannel(String path) { return Option$.MODULE$.empty(); }

        @Override
        public Option<OutputHandle> openOutputHandle(int id, String path, Mode mode) {
            return Option$.MODULE$.apply(new TestOutputHandle(this, id, path, mode));
        }
    }

    private static final class TestOutputHandle extends OutputHandle {
        private long position;

        private TestOutputHandle(OutputHandleOwner owner, int handle, String path, Mode mode) {
            super(owner, handle, path, mode);
        }

        @Override public long length() { return 8; }
        @Override public long position() { return position; }
        @Override public long seek(long value) { position = value; return value; }
        @Override public int read(byte[] into) { return 0; }
        @Override public void write(byte[] value) { position += value.length; }
    }
}
