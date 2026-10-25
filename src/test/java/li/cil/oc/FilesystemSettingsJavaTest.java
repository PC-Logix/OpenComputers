package li.cil.oc;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class FilesystemSettingsJavaTest {
    @Test
    public void clampsLimitsAndPreservesConfiguredDriveTiers() {
        var config = ConfigFactory.parseString("""
            filesystem {
              fileCost = -5
              bufferChanges = true
              hddSizes = [10, 20, 30, 40]
              ssdSizes = [50, 60, 70]
              hddPlatterCounts = [1, 2, 3, 4]
              floppySize = -1
              tmpSize = 4096
              maxHandles = -2
              maxReadBuffer = -3
              sectorSeekThreshold = -4
              sectorSeekTime = -0.5
            }
            """);

        FilesystemSettingsJava settings = new FilesystemSettingsJava(config);

        assertEquals(0, settings.fileCost);
        assertTrue(settings.bufferChanges);
        assertArrayEquals(new int[]{10, 20, 30, 40}, settings.hddSizes);
        assertArrayEquals(new int[]{50, 60, 70}, settings.ssdSizes);
        assertArrayEquals(new int[]{1, 2, 3, 4}, settings.hddPlatterCounts);
        assertEquals(0, settings.floppySize);
        assertEquals(4096, settings.tmpSize);
        assertEquals(0, settings.maxHandles);
        assertEquals(0, settings.maxReadBuffer);
        assertEquals(-4, settings.sectorSeekThreshold);
        assertEquals(-0.5, settings.sectorSeekTime, 0.0);
    }

    @Test
    public void usesDefaultsForInvalidDriveTierArrays() {
        var config = ConfigFactory.parseString("""
            filesystem {
              fileCost = 1
              bufferChanges = false
              hddSizes = [1]
              ssdSizes = [1]
              hddPlatterCounts = [1]
              floppySize = 1
              tmpSize = 1
              maxHandles = 1
              maxReadBuffer = 1
              sectorSeekThreshold = 1
              sectorSeekTime = 1.0
            }
            """);

        FilesystemSettingsJava settings = new FilesystemSettingsJava(config);

        assertArrayEquals(new int[]{1024, 2048, 4096, 8192}, settings.hddSizes);
        assertArrayEquals(new int[]{4096, 8192, 16384}, settings.ssdSizes);
        assertArrayEquals(new int[]{2, 4, 8, 12}, settings.hddPlatterCounts);
    }
}
