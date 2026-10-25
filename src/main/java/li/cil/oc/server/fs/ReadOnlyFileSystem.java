package li.cil.oc.server.fs;

import java.io.File;

/** Read-only filesystem rooted at an unpacked resource directory. */
public final class ReadOnlyFileSystem extends JavaInputStreamFileSystem implements FileInputStreamFileSystem {
    private final File root;
    private volatile Long cachedSpaceUsed;

    public ReadOnlyFileSystem(File root) {
        this.root = root;
    }

    @Override public File root() { return root; }

    @Override
    public long cachedSpaceUsed() {
        Long cached = cachedSpaceUsed;
        if (cached != null) return cached;
        synchronized (this) {
            if (cachedSpaceUsed == null) cachedSpaceUsed = FileInputFileSystemJava.spaceUsed(root);
            return cachedSpaceUsed;
        }
    }
}
