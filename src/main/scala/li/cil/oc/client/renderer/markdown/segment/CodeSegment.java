package li.cil.oc.client.renderer.markdown.segment;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.client.renderer.TextBufferRenderCache;
import li.cil.oc.client.renderer.markdown.MarkupFormat$;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import scala.Enumeration;
import scala.Option;
import scala.collection.immutable.Set;

public class CodeSegment implements BasicTextSegment {
    private final Segment parent;
    private final String text;
    private Segment next;
    private Set<Object> breaks;
    private Set<String> lists;

    public CodeSegment(Segment parent, String text) {
        this.parent = parent;
        this.text = text;
        BasicTextSegment.$init$(this);
    }

    @Override
    public Segment parent() {
        return parent;
    }

    @Override
    public String text() {
        return text;
    }

    @Override
    public Segment next() {
        return next;
    }

    @Override
    public void next_$eq(Segment next) {
        this.next = next;
    }

    @Override
    public Set<Object> breaks() {
        return breaks;
    }

    @Override
    public Set<String> lists() {
        return lists;
    }

    @Override
    public void li$cil$oc$client$renderer$markdown$segment$BasicTextSegment$_setter_$breaks_$eq(Set<Object> value) {
        breaks = value;
    }

    @Override
    public void li$cil$oc$client$renderer$markdown$segment$BasicTextSegment$_setter_$lists_$eq(Set<String> value) {
        lists = value;
    }

    @Override
    public Option<InteractiveSegment> render(GuiGraphics graphics, int x, int y, int indent, int maxWidth,
                                             Font renderer, int mouseX, int mouseY) {
        TextBufferRenderCache.renderer().generateChars(text.toCharArray());
        int currentX = x + indent;
        int currentY = y;
        String chars = text;
        int wrapIndent = computeWrapIndent(renderer);
        int numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer);
        while (!chars.isEmpty()) {
            String part = chars.substring(0, Math.min(numChars, chars.length()));
            RenderSystem.setShaderColor(0.75f, 0.8f, 1, 1);
            TextBufferRenderCache.renderer().drawString(graphics.pose(), part, currentX, currentY);
            currentX = x + wrapIndent;
            currentY += lineHeight(renderer);
            chars = chars.substring(Math.min(numChars, chars.length())).stripLeading();
            numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer);
        }
        return Option.empty();
    }

    @Override
    public boolean ignoreLeadingWhitespace() {
        return false;
    }

    @Override
    public int stringWidth(String value, Font renderer) {
        return value.length() * TextBufferRenderCache.renderer().charRenderWidth();
    }

    @Override
    public String toString(Enumeration.Value format) {
        if (format == MarkupFormat$.MODULE$.Markdown()) return "`" + text + "`";
        return "[prefix{1}]" + text + " [prefix{}]";
    }

    @Override
    public String toString() {
        return toString(MarkupFormat$.MODULE$.Markdown());
    }
}
