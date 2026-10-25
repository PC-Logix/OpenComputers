import li.cil.oc.client.Manual;
import li.cil.oc.client.ManualHistory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ManualTest {
    @Test
    public void resolvesRelativeAndRootedPageLinks() {
        assertEquals("guide/intro.md", Manual.makeRelative("intro.md", "guide/index.md"));
        assertEquals("/shared/intro.md", Manual.makeRelative("/shared/intro.md", "guide/index.md"));
    }

    @Test
    public void manualHistoryUsesLastVisitedPageAsTop() {
        Manual.History history = new Manual.History();
        history.push(new ManualHistory("index.md"));
        history.push(new ManualHistory("guide.md"));

        assertEquals("guide.md", history.top().path());
        assertEquals("guide.md", history.pop().path());
        assertEquals("index.md", history.top().path());
    }

    @Test
    public void manualStartsAtLocalizedIndexPage() {
        assertEquals("%LANGUAGE%/index.md", Manual.history.top().path());
    }
}
