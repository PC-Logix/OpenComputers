package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.markdown.MarkupFormat$;
import net.minecraft.ChatFormatting;
import scala.Enumeration;

public class StrikethroughSegment extends TextSegment {
    public StrikethroughSegment(Segment parent, String text) {
        super(parent, text);
    }

    @Override
    public String format() {
        return ChatFormatting.STRIKETHROUGH.toString();
    }

    @Override
    public String toString(Enumeration.Value format) {
        if (format == MarkupFormat$.MODULE$.Markdown()) return "~~" + text() + "~~";
        return "[prefix{m}]" + text() + " [prefix{}]";
    }
}
