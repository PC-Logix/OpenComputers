package li.cil.oc.server.fs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import li.cil.oc.api.fs.Mode;
import scala.Option;
import scala.Option$;

/** Java file operations used by writable file-backed filesystems. */
public final class FileOutputFileSystemJava {
    private FileOutputFileSystemJava() { }

    public static Option<OutputHandle> openOutputHandle(OutputStreamFileSystem owner, File root,
                                                        int id, String path, Mode mode) {
        if (!mode.isWritable()) throw new IllegalArgumentException();
        try {
            RandomAccessFile file = new RandomAccessFile(new File(root, path), "rw");
            return Option$.MODULE$.apply(new RandomAccessOutputHandle(owner, id, path, mode, file));
        } catch (IOException error) {
            return FileOutputFileSystemJava.<RuntimeException, Option<OutputHandle>>throwUnchecked(error);
        }
    }

    public static void saveData(File root, Runnable superSave) {
        superSave.run();
        updateRoot(root);
    }

    public static boolean delete(File root, String path) {
        File file = new File(root, path);
        return file.equals(root) || file.delete();
    }

    public static boolean makeDirectory(File root, String path) {
        return new File(root, path).mkdir();
    }

    public static boolean rename(File root, String from, String to) {
        try {
            Files.move(new File(root, from).toPath(), new File(root, to).toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public static boolean setLastModified(File root, String path, long time) {
        return new File(root, path).setLastModified(time);
    }

    public static void updateRoot(File root) {
        root.mkdirs();
        root.setLastModified(System.currentTimeMillis());
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }
}
