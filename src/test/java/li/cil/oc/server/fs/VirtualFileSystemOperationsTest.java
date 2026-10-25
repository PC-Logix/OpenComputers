package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import scala.Option;
import scala.Option$;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class VirtualFileSystemOperationsTest {
    @Test
    public void openOutputAndInputChannelsUseTheVirtualTree() throws Exception {
        TestFileSystem filesystem = new TestFileSystem();
        filesystem.root.makeDirectory("folder");
        Option<OutputHandle> opened = VirtualFileSystemJava.openOutputHandle(filesystem, 11, "folder/data", Mode.ReadWrite);
        assertTrue(opened.isDefined());
        OutputHandle handle = opened.get();
        handle.write(new byte[]{3, 5, 8});
        handle.close();

        Option<InputChannel> channel = VirtualFileSystemJava.openInputChannel(filesystem, "folder/data");
        assertTrue(channel.isDefined());
        byte[] contents = new byte[3];
        assertTrue(channel.get().read(contents) > 0);
        assertArrayEquals(new byte[]{3, 5, 8}, contents);
        channel.get().close();
    }

    @Test
    public void anOpenVirtualFileCannotBeOpenedForOutputAgain() {
        TestFileSystem filesystem = new TestFileSystem();
        Option<OutputHandle> first = VirtualFileSystemJava.openOutputHandle(filesystem, 1, "data", Mode.Write);
        assertTrue(first.isDefined());
        assertFalse(VirtualFileSystemJava.openOutputHandle(filesystem, 2, "data", Mode.Write).isDefined());
        first.get().close();
    }

    @Test
    public void virtualTreePersistsBeforeTheSuperclassLoadCallback() {
        TestFileSystem source = new TestFileSystem();
        source.root.makeDirectory("folder");
        source.root.touch("file");
        CompoundTag saved = new CompoundTag();
        AtomicBoolean saveCalled = new AtomicBoolean();
        VirtualFileSystemJava.saveData(source, saved, () -> saveCalled.set(true));
        assertTrue(saveCalled.get());

        TestFileSystem restored = new TestFileSystem();
        AtomicBoolean loadCalledAfterRestore = new AtomicBoolean();
        VirtualFileSystemJava.loadData(restored, saved, () -> {
            assertTrue(VirtualFileSystemJava.get(restored, "folder").isPresent());
            assertTrue(VirtualFileSystemJava.get(restored, "file").isPresent());
            loadCalledAfterRestore.set(true);
        });
        assertTrue(loadCalledAfterRestore.get());
    }

    @Test
    public void bufferedFilesystemLeavesTreePersistenceToItsBufferedSuperclass() {
        TestBufferedFileSystem filesystem = new TestBufferedFileSystem();
        CompoundTag saved = new CompoundTag();
        VirtualFileSystemJava.saveData(filesystem, saved, () -> { });
        assertFalse(saved.contains("root"));
    }

    private static class TestFileSystem extends JavaOutputStreamFileSystem implements VirtualFileSystem {
        private final VirtualDirectory root = new VirtualDirectory();

        @Override public VirtualDirectory root() { return root; }
        @Override public long spaceTotal() { return 1024; }
        @Override public long spaceUsed() { return 0; }
        @Override public Option<InputChannel> openInputChannel(String path) {
            return VirtualFileSystemJava.openInputChannel(this, path);
        }
        @Override public Option<OutputHandle> openOutputHandle(int id, String path, Mode mode) {
            return VirtualFileSystemJava.openOutputHandle(this, id, path, mode);
        }
    }

    private static final class TestBufferedFileSystem extends TestFileSystem implements Buffered {
        private final BufferedPersistence persistence = new BufferedPersistence();
        @Override public File fileRoot() { return new File("."); }
        @Override public BufferedPersistence persistence() { return persistence; }
    }
}
