package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.markdown.Document;
import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextSegment implements BasicTextSegment {
    private final Segment parent;
    private final String text;
    private Segment next;
    private boolean interactiveResolved;
    private Optional<InteractiveSegment> interactive;

    public TextSegment(Segment parent, String text) {
        this.parent = parent;
        this.text = text;
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
    public Optional<InteractiveSegment> render(GuiGraphics graphics, int x, int y, int indent, int maxWidth,
                                             Font renderer, int mouseX, int mouseY) {
        int currentX = x + indent;
        int currentY = y;
        String chars = indent == 0 ? text.stripLeading() : text;
        int wrapIndent = computeWrapIndent(renderer);
        int numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer);
        Optional<InteractiveSegment> hovered = Optional.empty();

        var stack = graphics.pose();
        while (!chars.isEmpty()) {
            String part = chars.substring(0, Math.min(numChars, chars.length()));
            if (hovered.isEmpty() && resolvedInteractive().isPresent()) {
                hovered = resolvedInteractive().get().checkHovered(mouseX, mouseY, currentX, currentY,
                    stringWidth(part, renderer), (int) (Document.lineHeight(renderer) * resolvedScale()));
            }
            stack.pushPose();
            stack.translate(currentX, currentY, 0);
            stack.scale(resolvedScale(), resolvedScale(), resolvedScale());
            stack.translate(-currentX, -currentY, 0);
            graphics.drawString(renderer, resolvedFormat() + part, currentX, currentY, resolvedColor());
            stack.popPose();
            currentX = x + wrapIndent;
            currentY += lineHeight(renderer);
            chars = chars.substring(Math.min(numChars, chars.length())).stripLeading();
            numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer);
        }
        return hovered;
    }

    @Override
    public List<Segment> refine(Pattern pattern, BiFunction<Segment, Matcher, Segment> factory) {
        List<Segment> result = new ArrayList<>();
        int textStart = 0;
        Matcher matches = pattern.matcher(text);
        while (matches.find()) {
            if (matches.start() > textStart) {
                result.add(new TextSegment(this, text.substring(textStart, matches.start())));
            }
            textStart = matches.end();
            result.add(factory.apply(this, matches));
        }
        if (textStart == 0) {
            result.add(this);
        } else if (textStart < text.length()) {
            result.add(new TextSegment(this, text.substring(textStart)));
        }
        return result;
    }

    @Override
    public int lineHeight(Font renderer) {
        return (int) (BasicTextSegment.super.lineHeight(renderer) * resolvedScale());
    }

    @Override
    public int stringWidth(String value, Font renderer) {
        return (int) (renderer.width(resolvedFormat() + value) * resolvedScale());
    }

    public Optional<Object> color() {
        return Optional.empty();
    }

    public Optional<Object> scale() {
        return Optional.empty();
    }

    public String format() {
        return "";
    }

    private int resolvedColor() {
        if (color().isPresent()) return (Integer) color().get();
        return parent instanceof TextSegment segment ? segment.resolvedColor() : 0xDDDDDD;
    }

    private float resolvedScale() {
        if (parent instanceof TextSegment segment) {
            float local = scale().isPresent() ? (Float) scale().get() : 1f;
            return local * segment.resolvedScale();
        }
        return 1f;
    }

    private String resolvedFormat() {
        return parent instanceof TextSegment segment ? segment.resolvedFormat() + format() : format();
    }

    private Optional<InteractiveSegment> resolvedInteractive() {
        if (!interactiveResolved) {
            if (this instanceof InteractiveSegment segment) interactive = Optional.of(segment);
            else if (parent instanceof TextSegment segment) interactive = segment.resolvedInteractive();
            else interactive = Optional.empty();
            interactiveResolved = true;
        }
        return interactive;
    }

    @Override
    public String toString(MarkupFormat format) {
        return text;
    }

    @Override
    public String toString() {
        return toString(MarkupFormat.Markdown);
    }
}
