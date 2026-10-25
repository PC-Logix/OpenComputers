package li.cil.oc.server.fs;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import static org.junit.Assert.*;

public class VirtualFileSystemTreeTest {
    @Test
    public void directoryTreeRoundTripsNamesDataAndModificationTimes() {
        VirtualFileSystem.VirtualDirectory root = new VirtualFileSystem.VirtualDirectory();
        assertTrue(root.makeDirectory("folder"));
        VirtualFileSystem.VirtualDirectory folder = (VirtualFileSystem.VirtualDirectory) root.children().get("folder");
        VirtualFileSystem.VirtualFile file = folder.touch("payload").get();
        file.data().add((byte) 7);
        file.data().add((byte) 11);
        file.setLastModified(1234L);

        CompoundTag saved = new CompoundTag();
        root.saveData(saved);
        VirtualFileSystem.VirtualDirectory restored = new VirtualFileSystem.VirtualDirectory();
        restored.loadData(saved);

        VirtualFileSystem.VirtualDirectory restoredFolder = (VirtualFileSystem.VirtualDirectory) restored.children().get("folder");
        VirtualFileSystem.VirtualFile restoredFile = restoredFolder.touch("payload").get();
        byte[] restoredBytes = new byte[restoredFile.data().size()];
        for (int index = 0; index < restoredBytes.length; index++) restoredBytes[index] = restoredFile.data().get(index);
        assertArrayEquals(new byte[]{7, 11}, restoredBytes);
        assertEquals(1234L, restoredFile.lastModified());
        assertArrayEquals(new String[]{"folder/"}, restored.list());
    }

    @Test
    public void openFileCannotBeDeletedUntilItsHandleCloses() {
        VirtualFileSystem.VirtualDirectory root = new VirtualFileSystem.VirtualDirectory();
        VirtualFileSystem.VirtualFile file = root.touch("payload").get();
        Object openHandle = new Object();
        file.setHandle(openHandle);

        assertFalse(root.delete("payload"));
        file.setHandle(null);
        assertTrue(root.delete("payload"));
    }
}
