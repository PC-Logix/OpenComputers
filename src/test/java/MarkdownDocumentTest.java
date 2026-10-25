import li.cil.oc.client.renderer.markdown.Document;
import li.cil.oc.client.renderer.markdown.MarkupFormat;
import li.cil.oc.client.renderer.markdown.segment.Segment;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MarkdownDocumentTest {
    @Test
    public void parsesInlineFormattingAndPreservesFencedCode() {
        Segment document = Document.parse(List.of(
            "## Hello **world**",
            "text [site](https://example.com)",
            "```",
            "a *literal* line",
            "```"
        ));

        assertEquals("HeaderSegment", document.parent().getClass().getSimpleName());
        assertEquals("## Hello **world**", document.parent().toString(MarkupFormat.Markdown));
        List<String> actual = new ArrayList<>();
        actual.addAll(document.renderAsText(MarkupFormat.Markdown));
        assertEquals(List.of(
            "Hello **world**",
            "text [site](https://example.com)",
            "```\na *literal* line\n```"
        ), actual);
    }
}
