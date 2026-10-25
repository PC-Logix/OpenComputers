package li.cil.oc.client.renderer.markdown;

import li.cil.oc.api.Manual;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.client.renderer.markdown.segment.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small Markdown parser used to render documentation in the manual. */
public final class Document {
    private record SegmentType(Pattern pattern, BiFunction<Segment, Matcher, Segment> factory) {}

    private static final List<SegmentType> SEGMENT_TYPES = List.of(
        new SegmentType(regex("^(#+)\\s(.*)"), (segment, match) -> new HeaderSegment(segment, match.group(2), match.group(1).length())),
        new SegmentType(regex("(`)(.*?)\\1"), (segment, match) -> new CodeSegment(segment, match.group(2))),
        new SegmentType(regex("!\\[([^\\[]*)\\]\\(([^\\)]+)\\)"), Document::imageSegment),
        new SegmentType(regex("\\[([^\\[]+)\\]\\(([^\\)]+)\\)"), (segment, match) -> new LinkSegment(segment, match.group(1), match.group(2))),
        new SegmentType(regex("(\\*\\*|__)(\\S.*?\\S|$)\\1"), (segment, match) -> new BoldSegment(segment, match.group(2))),
        new SegmentType(regex("(\\*|_)(\\S.*?\\S|$)\\1"), (segment, match) -> new ItalicSegment(segment, match.group(2))),
        new SegmentType(regex("~~(\\S.*?\\S|$)~~"), (segment, match) -> new StrikethroughSegment(segment, match.group(1)))
    );

    private static final Pattern FENCE = regex("^\\s*```(?:.*)?\\s*$");

    private static Pattern regex(String expression) {
        return Pattern.compile(expression);
    }

    private Document() {}

    public static Segment parse(Iterable<String> document) {
        List<Segment> segments = parseBlocks(document);
        for (SegmentType type : SEGMENT_TYPES) {
            List<Segment> refined = new ArrayList<>();
            for (Segment segment : segments) {
                refined.addAll(segment.refine(type.pattern(), type.factory()));
            }
            segments = refined;
        }

        Iterator<Segment> iterator = segments.iterator();
        if (!iterator.hasNext()) throw new java.util.NoSuchElementException("head of empty document");
        Segment first = iterator.next();
        Segment previous = first;
        while (iterator.hasNext()) {
            Segment next = iterator.next();
            previous.next_$eq(next);
            previous = next;
        }
        return first;
    }

    private static List<Segment> parseBlocks(Iterable<String> document) {
        List<Segment> result = new ArrayList<>();
        List<String> pending = new ArrayList<>();
        Iterator<String> lines = document.iterator();

        while (lines.hasNext()) {
            String line = lines.next();
            if (FENCE.matcher(line).matches()) {
                addText(result, pending);
                List<String> code = new ArrayList<>();
                boolean closed = false;
                while (lines.hasNext() && !closed) {
                    String codeLine = lines.next();
                    if (FENCE.matcher(codeLine).matches()) closed = true;
                    else code.add(codeLine);
                }
                result.add(new CodeBlockSegment(null, code));
            } else {
                pending.add(line);
            }
        }

        addText(result, pending);
        return result;
    }

    private static void addText(List<Segment> result, List<String> pending) {
        for (String line : pending) result.add(new TextSegment(null, stripTrailing(line)));
        pending.clear();
    }

    private static String stripTrailing(String value) {
        if (value == null) return "";
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) end--;
        return value.substring(0, end);
    }

    private static Segment imageSegment(Segment parent, Matcher match) {
        try {
            ImageRenderer renderer = Manual.imageFor(match.group(2));
            return renderer != null
                ? new RenderSegment(parent, match.group(1), renderer)
                : new TextSegment(parent, "No renderer found for: " + match.group(2));
        } catch (Throwable error) {
            return new TextSegment(parent, error.toString() != null ? error.toString() : "Unknown error.");
        }
    }

    public static int height(Segment document, int maxWidth, Font renderer) {
        int currentX = 0;
        int currentY = 0;
        Segment segment = document;
        while (segment != null) {
            currentY += segment.nextY(currentX, maxWidth, renderer);
            currentX = segment.nextX(currentX, maxWidth, renderer);
            segment = segment.next();
        }
        return currentY;
    }

    public static int lineHeight(Font renderer) {
        return renderer.lineHeight + 1;
    }

    public static Optional<InteractiveSegment> render(GuiGraphics graphics, Segment document, int x, int y,
            int maxWidth, int maxHeight, int yOffset, Font renderer, int mouseX, int mouseY) {
        graphics.enableScissor(x, y, x + maxWidth, y + maxHeight);

        Optional<InteractiveSegment> hovered = Optional.empty();
        int indent = 0;
        int currentY = y - yOffset;
        int minY = y - lineHeight(renderer);
        int maxY = y + maxHeight + lineHeight(renderer);
        Segment segment = document;
        while (segment != null) {
            int segmentHeight = segment.nextY(indent, maxWidth, renderer);
            if (currentY + segmentHeight >= minY && currentY <= maxY) {
                Optional<InteractiveSegment> result = segment.render(graphics, x, currentY, indent, maxWidth,
                    renderer, mouseX, mouseY);
                if (hovered.isEmpty()) hovered = result;
            }
            currentY += segmentHeight;
            indent = segment.nextX(indent, maxWidth, renderer);
            segment = segment.next();
        }
        if (mouseX < x || mouseX > x + maxWidth || mouseY < y || mouseY > y + maxHeight) {
            hovered = Optional.empty();
        }
        hovered.ifPresent(InteractiveSegment::notifyHover);
        graphics.disableScissor();
        return hovered;
    }
}
