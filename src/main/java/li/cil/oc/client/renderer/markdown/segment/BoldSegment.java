package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.ChatFormatting;

public class BoldSegment extends TextSegment {
    public BoldSegment(Segment parent, String text) {
        super(parent, text);
    }

    @Override
    public String format() {
        return ChatFormatting.BOLD.toString();
    }

    @Override
    public String toString(MarkupFormat format) {
        if (format == MarkupFormat.Markdown) return "**" + text() + "**";
        return "[prefix{l}]" + text() + " [prefix{}]";
    }
}
