package li.cil.oc;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ChunkloaderSettingsJavaTest {
    @Test
    public void preservesConfiguredDimensionListsAndEmptyFallback() {
        var config = ConfigFactory.parseString("""
            chunkloader.dimBlacklist = [-1, 2]
            """);

        ChunkloaderSettingsJava settings = new ChunkloaderSettingsJava(config);

        assertEquals(List.of(-1, 2), settings.dimensionBlacklist);
        assertTrue(settings.dimensionWhitelist instanceof LinkedList<?>);
        assertTrue(settings.dimensionWhitelist.isEmpty());
    }
}
