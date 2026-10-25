package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import scala.Option;
import scala.Option$;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public final class InputStreamFileSystemSupportTest {
    @Test
    public void handlePositionSurvivesPersistenceAndCloseRemovesIt() throws Exception {
        MemoryFileSystem source = new MemoryFileSystem();
        int handle = source.open("/file", Mode.Read);
        byte[] first = new byte[2];
        assertEquals(2, source.getHandle(handle).read(first));
        assertArrayEquals(new byte[]{1, 2}, first);

        CompoundTag saved = new CompoundTag();
        source.saveData(saved);
        source.close();
        assertNull(source.getHandle(handle));

        MemoryFileSystem restored = new MemoryFileSystem();
        restored.loadData(saved);
        assertNotNull(restored.getHandle(handle));
        byte[] remaining = new byte[2];
        assertEquals(2, restored.getHandle(handle).read(remaining));
        assertArrayEquals(new byte[]{3, 4}, remaining);
    }

    @Test(expected = FileNotFoundException.class)
    public void openRejectsUnknownPaths() throws Exception {
        new MemoryFileSystem().open("/missing", Mode.Read);
    }

    private static final class MemoryFileSystem extends JavaInputStreamFileSystem {
        @Override public long spaceTotal() { return 4; }
        @Override public long spaceUsed() { return 4; }
        @Override public boolean exists(String path) { return path.equals("/file"); }
        @Override public long size(String path) { return exists(path) ? 4 : 0; }
        @Override public boolean isDirectory(String path) { return false; }
        @Override public long lastModified(String path) { return 0; }
        @Override public String[] list(String path) { return null; }
        @Override public Option<InputChannel> openInputChannel(String path) {
            return exists(path)
                    ? Option$.MODULE$.apply(new InputStreamChannel(new ByteArrayInputStream(new byte[]{1, 2, 3, 4})))
                    : Option$.MODULE$.empty();
        }
    }
}
