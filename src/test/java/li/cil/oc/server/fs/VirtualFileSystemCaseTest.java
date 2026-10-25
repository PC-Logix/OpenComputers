package li.cil.oc.server.fs;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public final class VirtualFileSystemCaseTest {
    @Test
    public void resolvesExistingDirectoryAndFileNamesIgnoringCase() {
        VirtualFileSystem.VirtualDirectory root = new VirtualFileSystem.VirtualDirectory();
        root.makeDirectory("Mixed");
        VirtualFileSystem.VirtualDirectory mixed = (VirtualFileSystem.VirtualDirectory) root.children().get("Mixed");
        mixed.touch("Screen.dat");

        assertArrayEquals(new String[]{"Mixed", "Screen.dat"},
            VirtualFileSystemJava.toCaseInsensitive(root, new String[]{"mixed", "screen.DAT"}));
    }

    @Test
    public void retainsUnknownPathSegments() {
        VirtualFileSystem.VirtualDirectory root = new VirtualFileSystem.VirtualDirectory();

        assertArrayEquals(new String[]{"missing", "child"},
            VirtualFileSystemJava.toCaseInsensitive(root, new String[]{"missing", "child"}));
    }
}
