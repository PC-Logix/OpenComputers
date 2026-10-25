package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A rendered segment in a parsed manual document. */
public interface Segment {
    Segment parent();

    int nextX(int indent, int maxWidth, Font renderer);

    int nextY(int indent, int maxWidth, Font renderer);

    java.util.Optional<InteractiveSegment> render(GuiGraphics graphics, int x, int y, int indent, int maxWidth,
                                                    Font renderer, int mouseX, int mouseY);

    String toString(MarkupFormat format);

    Segment next();

    void next_$eq(Segment value);

    default Segment root() {
        Segment segment = this;
        while (segment.parent() != null) segment = segment.parent();
        return segment;
    }

    default List<String> renderAsText(MarkupFormat format) {
        Segment segment = this;
        List<String> result = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        while (segment != null) {
            builder.append(segment.toString(format));
            if (segment.isLast()) {
                result.add(builder.toString());
                builder.setLength(0);
            }
            segment = segment.next();
        }
        return result;
    }

    @Override
    String toString();

    default List<Segment> refine(Pattern pattern, BiFunction<Segment, Matcher, Segment> factory) {
        return List.of(this);
    }

    default boolean isLast() {
        Segment following = next();
        return following == null || root() != following.root();
    }
}
