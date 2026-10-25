package li.cil.oc.server.fs;

import java.io.File;
import java.io.FileNotFoundException;

/** Java file operations shared by file-backed read-only filesystems. */
public final class FileInputFileSystemJava {
    private FileInputFileSystemJava() { }

    public static long spaceUsed(File root) {
        if (root.isDirectory()) {
            File[] children = root.listFiles();
            long total = 0;
            for (File child : children) total += spaceUsed(child);
            return total;
        }
        return root.length();
    }

    public static boolean exists(File file) { return file.exists(); }
    public static long size(File file) { return file.isFile() ? file.length() : 0L; }
    public static boolean isDirectory(File file) { return file.isDirectory(); }
    public static long lastModified(File file) { return file.lastModified(); }

    public static String[] list(File file, String path) throws FileNotFoundException {
        if (file.exists() && file.isFile()) return new String[]{file.getName()};
        if (file.exists() && file.isDirectory() && file.list() != null) {
            File[] children = file.listFiles();
            String[] result = new String[children.length];
            for (int index = 0; index < children.length; index++) {
                File child = children[index];
                result[index] = child.isDirectory() ? child.getName() + "/" : child.getName();
            }
            return result;
        }
        throw new FileNotFoundException("no such file or directory: " + path);
    }
}
