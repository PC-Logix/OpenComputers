package li.cil.oc;

import com.typesafe.config.Config;

import java.util.List;

/** Filesystem sizing and I/O configuration. */
public final class FilesystemSettingsJava {
    public final int fileCost;
    public final boolean bufferChanges;
    public final int[] hddSizes;
    public final int[] ssdSizes;
    public final int[] hddPlatterCounts;
    public final int floppySize;
    public final int tmpSize;
    public final int maxHandles;
    public final int maxReadBuffer;
    public final int sectorSeekThreshold;
    public final double sectorSeekTime;

    public FilesystemSettingsJava(Config config) {
        fileCost = Math.max(0, config.getInt("filesystem.fileCost"));
        bufferChanges = config.getBoolean("filesystem.bufferChanges");
        hddSizes = readIntArray(config, "filesystem.hddSizes", 4,
            new int[]{1024, 2048, 4096, 8192}, "Bad number of HDD sizes, ignoring.");
        ssdSizes = readIntArray(config, "filesystem.ssdSizes", 3,
            new int[]{4096, 8192, 16384}, "Bad number of SSD sizes, ignoring.");
        hddPlatterCounts = readIntArray(config, "filesystem.hddPlatterCounts", 4,
            new int[]{2, 4, 8, 12}, "Bad number of HDD platter counts, ignoring.");
        floppySize = Math.max(0, config.getInt("filesystem.floppySize"));
        tmpSize = Math.max(0, config.getInt("filesystem.tmpSize"));
        maxHandles = Math.max(0, config.getInt("filesystem.maxHandles"));
        maxReadBuffer = Math.max(0, config.getInt("filesystem.maxReadBuffer"));
        sectorSeekThreshold = config.getInt("filesystem.sectorSeekThreshold");
        sectorSeekTime = config.getDouble("filesystem.sectorSeekTime");
    }

    private static int[] readIntArray(Config config, String path, int expectedLength,
                                      int[] fallback, String warning) {
        List<Integer> values = config.getIntList(path);
        if (values.size() != expectedLength) {
            OpenComputers.log().warn(warning);
            return fallback;
        }
        int[] result = new int[expectedLength];
        for (int i = 0; i < expectedLength; i++) result[i] = values.get(i);
        return result;
    }
}
