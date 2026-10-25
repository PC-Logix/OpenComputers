package li.cil.oc.server.fs;

/** Writable file-backed operations layered over the stream handle filesystem. */
public interface FileOutputStreamFileSystem extends FileInputStreamFileSystem, OutputStreamFileSystem {
    @Override
    default long spaceTotal() { return -1; }

    @Override
    default long spaceUsed() { return -1; }

    @Override
    default boolean delete(String path) {
        return FileOutputFileSystemJava.delete(root(), FileSystemPathJava.validatePath(path));
    }

    @Override
    default boolean makeDirectory(String path) {
        return FileOutputFileSystemJava.makeDirectory(root(), FileSystemPathJava.validatePath(path));
    }

    @Override
    default boolean rename(String from, String to) {
        return FileOutputFileSystemJava.rename(root(), FileSystemPathJava.validatePath(from), FileSystemPathJava.validatePath(to));
    }

    @Override
    default boolean setLastModified(String path, long time) {
        return FileOutputFileSystemJava.setLastModified(root(), FileSystemPathJava.validatePath(path), time);
    }
}
