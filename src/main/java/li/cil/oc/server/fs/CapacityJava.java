package li.cil.oc.server.fs;

/** Recursive size accounting shared by capacity-limited filesystems. */
public final class CapacityJava {
    private CapacityJava() {
    }

    public static long computeSize(Capacity filesystem, String path, long fileCost) {
        long total = fileCost + filesystem.size(path);
        if (filesystem.isDirectory(path)) {
            for (String child : filesystem.list(path)) {
                total += computeSize(filesystem, path + child, fileCost);
            }
        }
        return total;
    }
}
