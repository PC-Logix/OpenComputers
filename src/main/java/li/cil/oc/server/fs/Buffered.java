package li.cil.oc.server.fs;

import li.cil.oc.util.SafeThreadPool;
import li.cil.oc.util.ThreadPoolFactory;
import java.io.File;

/** Filesystem whose virtual contents are asynchronously mirrored to disk. */
public interface Buffered extends OutputStreamFileSystem {
    SafeThreadPool FILE_SAVE_HANDLER = ThreadPoolFactory.createSafePool("FileSystem", 1);

    File fileRoot();
    BufferedPersistence persistence();

    static SafeThreadPool fileSaveHandler() { return FILE_SAVE_HANDLER; }
}
