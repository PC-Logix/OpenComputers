package li.cil.oc.server.fs;

import java.io.File;
import java.io.IOException;
import scala.Option;
import scala.Option$;

/** File-backed read-only operations layered over input-stream handle management. */
public interface FileInputStreamFileSystem extends InputStreamFileSystem {
    File root();

    @Override
    default Option<InputChannel> openInputChannel(String path) {
        try {
            return Option$.MODULE$.apply(new FileInputChannel(file(path)));
        } catch (IOException error) {
            return FileInputStreamFileSystem.<RuntimeException, Option<InputChannel>>throwUnchecked(error);
        }
    }

    @Override
    default long spaceTotal() {
        return spaceUsed();
    }

    @Override
    default long spaceUsed() {
        return cachedSpaceUsed();
    }

    default long cachedSpaceUsed() {
        return FileInputFileSystemJava.spaceUsed(root());
    }

    @Override
    default boolean exists(String path) {
        return FileInputFileSystemJava.exists(file(path));
    }

    @Override
    default long size(String path) {
        return FileInputFileSystemJava.size(file(path));
    }

    @Override
    default boolean isDirectory(String path) {
        return FileInputFileSystemJava.isDirectory(file(path));
    }

    @Override
    default long lastModified(String path) {
        return FileInputFileSystemJava.lastModified(file(path));
    }

    @Override
    default String[] list(String path) {
        try {
            return FileInputFileSystemJava.list(file(path), path);
        } catch (IOException error) {
            return FileInputStreamFileSystem.<RuntimeException, String[]>throwUnchecked(error);
        }
    }

    default File file(String path) {
        return new File(root(), FileSystemPathJava.validatePath(path));
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }
}
