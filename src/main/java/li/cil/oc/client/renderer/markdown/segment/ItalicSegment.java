package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.ChatFormatting;

public class ItalicSegment extends TextSegment {
    public ItalicSegment(Segment parent, String text) {
        super(parent, text);
    }

    @Override
    public String format() {
        return ChatFormatting.ITALIC.toString();
    }

    @Override
    public String toString(MarkupFormat format) {
        if (format == MarkupFormat.Markdown) return "*" + text() + "*";
        return "[prefix{o}]" + text() + " [prefix{}]";
    }
}
