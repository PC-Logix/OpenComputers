package li.cil.oc.client.renderer.markdown.segment;

import li.cil.oc.client.renderer.markdown.Document;
import li.cil.oc.client.renderer.markdown.MarkupFormat;
import net.minecraft.client.gui.Font;

public interface BasicTextSegment extends Segment {
    String BREAKS = " .,;:!?_=-+*/\\";

    String text();

    int stringWidth(String value, Font renderer);

    default boolean ignoreLeadingWhitespace() {
        return true;
    }

    default int lineHeight(Font renderer) {
        return Document.lineHeight(renderer);
    }

    @Override
    default int nextX(int indent, int maxWidth, Font renderer) {
        if (isLast()) return 0;
        int currentX = indent;
        String chars = text();
        if (ignoreLeadingWhitespace() && indent == 0) chars = chars.stripLeading();
        int wrapIndent = computeWrapIndent(renderer);
        int numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer);
        while (chars.length() > numChars) {
            chars = chars.substring(Math.min(numChars, chars.length())).stripLeading();
            numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer);
            currentX = wrapIndent;
        }
        return currentX + stringWidth(chars, renderer);
    }

    @Override
    default int nextY(int indent, int maxWidth, Font renderer) {
        int lines = 0;
        String chars = text();
        if (ignoreLeadingWhitespace() && indent == 0) chars = chars.stripLeading();
        int wrapIndent = computeWrapIndent(renderer);
        int numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer);
        while (chars.length() > numChars) {
            lines++;
            chars = chars.substring(Math.min(numChars, chars.length())).stripLeading();
            numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer);
        }
        if (isLast()) lines++;
        return lines * lineHeight(renderer);
    }

    @Override
    default String toString(MarkupFormat format) {
        return text();
    }

    default int maxChars(String value, int maxWidth, int maxLineWidth, Font renderer) {
        int pos = -1;
        int lastBreak = -1;
        int fullWidth = stringWidth(value, renderer);
        while (pos < value.length()) {
            pos++;
            int width = stringWidth(value.substring(0, pos), renderer);
            if (width >= maxWidth) {
                boolean mayUseFullLine = maxWidth == maxLineWidth;
                boolean canFitInLine = fullWidth <= maxLineWidth;
                boolean matchesFullLine = fullWidth == maxLineWidth;
                if (lastBreak >= 0) return lastBreak + 1;
                if (mayUseFullLine && matchesFullLine) return value.length();
                if (canFitInLine && !mayUseFullLine) return 0;
                return pos - 1;
            }
            if (pos < value.length() && BREAKS.indexOf(value.charAt(pos)) >= 0) lastBreak = pos;
        }
        return pos;
    }

    default int computeWrapIndent(Font renderer) {
        String rootPrefix = ((TextSegment) root()).text().substring(0, Math.min(2, ((TextSegment) root()).text().length()));
        return rootPrefix.equals("- ") || rootPrefix.equals("* ") ? renderer.width(rootPrefix) : 0;
    }
}
