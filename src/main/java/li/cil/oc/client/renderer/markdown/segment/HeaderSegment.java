package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.ChatFormatting;

import java.util.Optional;

public class HeaderSegment extends TextSegment {
    private final int level;
    private final float fontScale;

    public HeaderSegment(Segment parent, String text, int level) {
        super(parent, text);
        this.level = level;
        this.fontScale = Math.max(2, 5 - level) / 2f;
    }

    public int level() {
        return level;
    }

    @Override
    public Optional<Object> scale() {
        return Optional.of(fontScale);
    }

    @Override
    public String format() {
        return ChatFormatting.UNDERLINE.toString();
    }

    @Override
    public String toString(MarkupFormat format) {
        if (format == MarkupFormat.Markdown) return "#".repeat(Math.max(0, level)) + " " + text();
        return "[prefix{l}]" + text() + " [prefix{}]";
    }
}
