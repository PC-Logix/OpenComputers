package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.markdown.Document;
import li.cil.oc.client.renderer.markdown.MarkupFormat$;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import scala.Enumeration;
import scala.Function2;
import scala.Option;
import scala.collection.Iterable;
import scala.collection.immutable.Set;
import scala.collection.mutable.ArrayBuffer;
import scala.util.matching.Regex;

public class TextSegment implements BasicTextSegment {
    private final Segment parent;
    private final String text;
    private Segment next;
    private Set<Object> breaks;
    private Set<String> lists;
    private boolean interactiveResolved;
    private Option<InteractiveSegment> interactive;

    public TextSegment(Segment parent, String text) {
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
        int currentX = x + indent;
        int currentY = y;
        String chars = indent == 0 ? text.stripLeading() : text;
        int wrapIndent = computeWrapIndent(renderer);
        int numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer);
        Option<InteractiveSegment> hovered = Option.empty();

        var stack = graphics.pose();
        while (!chars.isEmpty()) {
            String part = chars.substring(0, Math.min(numChars, chars.length()));
            if (hovered.isEmpty() && resolvedInteractive().nonEmpty()) {
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
    public Iterable<Segment> refine(Regex pattern, Function2<Segment, Regex.Match, Segment> factory) {
        ArrayBuffer<Segment> result = new ArrayBuffer<>();
        int textStart = 0;
        var matches = pattern.findAllMatchIn(text);
        while (matches.hasNext()) {
            Regex.Match match = matches.next();
            if (match.start() > textStart) {
                result.addOne(new TextSegment(this, text.substring(textStart, match.start())));
            }
            textStart = match.end();
            result.addOne(factory.apply(this, match));
        }
        if (textStart == 0) {
            result.addOne(this);
        } else if (textStart < text.length()) {
            result.addOne(new TextSegment(this, text.substring(textStart)));
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

    public Option<Object> color() {
        return Option.empty();
    }

    public Option<Object> scale() {
        return Option.empty();
    }

    public String format() {
        return "";
    }

    private int resolvedColor() {
        if (color().nonEmpty()) return (Integer) color().get();
        return parent instanceof TextSegment segment ? segment.resolvedColor() : 0xDDDDDD;
    }

    private float resolvedScale() {
        if (parent instanceof TextSegment segment) {
            float local = scale().nonEmpty() ? (Float) scale().get() : 1f;
            return local * segment.resolvedScale();
        }
        return 1f;
    }

    private String resolvedFormat() {
        return parent instanceof TextSegment segment ? segment.resolvedFormat() + format() : format();
    }

    private Option<InteractiveSegment> resolvedInteractive() {
        if (!interactiveResolved) {
            if (this instanceof InteractiveSegment segment) interactive = Option.apply(segment);
            else if (parent instanceof TextSegment segment) interactive = segment.resolvedInteractive();
            else interactive = Option.empty();
            interactiveResolved = true;
        }
        return interactive;
    }

    @Override
    public String toString(Enumeration.Value format) {
        return text;
    }

    @Override
    public String toString() {
        return toString(MarkupFormat$.MODULE$.Markdown());
    }
}
