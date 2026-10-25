package li.cil.oc.common.component.traits;

import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.util.FontUtils;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.PackedColorColor;

/** Shared text-buffer behavior for screens and GPU video RAM. */
public interface TextBufferProxy extends TextBuffer {
    li.cil.oc.util.TextBuffer data();

    @Override default int getWidth() { return data().width(); }
    @Override default int getHeight() { return data().height(); }

    @Override default boolean setColorDepth(ColorDepth depth) {
        if (depth.ordinal() > getMaximumColorDepth().ordinal()) throw new IllegalArgumentException("unsupported depth");
        return data().format_$eq(PackedColor.formatForDepth(depth));
    }

    @Override default ColorDepth getColorDepth() { return data().format().depth(); }
    default void onBufferPaletteChange(int index) { }

    @Override default void setPaletteColor(int index, int color) {
        if (data().format() instanceof PackedColor.MutablePaletteFormat palette) {
            palette.update(index, color);
            onBufferPaletteChange(index);
        } else throw new RuntimeException("palette not available");
    }

    @Override default int getPaletteColor(int index) {
        if (data().format() instanceof PackedColor.MutablePaletteFormat palette) return palette.apply(index);
        throw new RuntimeException("palette not available");
    }

    default void onBufferColorChange() { }
    @Override default void setForegroundColor(int color) { setForegroundColor(color, false); }
    @Override default void setForegroundColor(int color, boolean fromPalette) {
        PackedColorColor next = new PackedColorColor(color, fromPalette);
        if (!data().foreground().equals(next)) {
            data().foreground_$eq(next);
            onBufferColorChange();
        }
    }
    @Override default int getForegroundColor() { return data().foreground().value(); }
    @Override default boolean isForegroundFromPalette() { return data().foreground().isPalette(); }
    @Override default void setBackgroundColor(int color) { setBackgroundColor(color, false); }
    @Override default void setBackgroundColor(int color, boolean fromPalette) {
        PackedColorColor next = new PackedColorColor(color, fromPalette);
        if (!data().background().equals(next)) {
            data().background_$eq(next);
            onBufferColorChange();
        }
    }
    @Override default int getBackgroundColor() { return data().background().value(); }
    @Override default boolean isBackgroundFromPalette() { return data().background().isPalette(); }

    default void onBufferCopy(int col, int row, int width, int height, int tx, int ty) { }
    @Override default void copy(int col, int row, int width, int height, int tx, int ty) {
        if (data().copy(col, row, width, height, tx, ty)) onBufferCopy(col, row, width, height, tx, ty);
    }
    default void onBufferFill(int col, int row, int width, int height, int value) { }
    @Override default void fill(int col, int row, int width, int height, char value) {
        fill(col, row, width, height, (int) value);
    }
    @Override default void fill(int col, int row, int width, int height, int value) {
        if (data().fill(col, row, width, height, value)) onBufferFill(col, row, width, height, value);
    }
    default void onBufferSet(int col, int row, String value, boolean vertical) { }

    private static String truncate(String value, int leftOffset, int maxWidth, boolean vertical) {
        if (maxWidth <= 0) return "";
        int start = 0, skipped = 0;
        while (start < value.length() && skipped < leftOffset) {
            int cp = value.codePointAt(start);
            int width = FontUtils.wcwidth(cp);
            skipped += width == 0 ? 0 : vertical ? 1 : width;
            start += Character.charCount(cp);
        }
        int end = start, width = 0;
        while (end < value.length()) {
            int cp = value.codePointAt(end);
            int cpWidth = FontUtils.wcwidth(cp);
            int cellWidth = cpWidth == 0 ? 0 : vertical ? 1 : cpWidth;
            if (cellWidth > 0 && width + cellWidth > maxWidth) break;
            width += cellWidth;
            end += Character.charCount(cp);
        }
        return value.substring(start, end);
    }

    @Override default void set(int col, int row, String value, boolean vertical) {
        li.cil.oc.util.TextBuffer data = data();
        if (col >= data.width()) return;
        int x, y;
        String clipped;
        if (vertical) {
            if (row < 0) { x = col; y = 0; clipped = truncate(value, -row, data.height(), true); }
            else { x = col; y = row; clipped = truncate(value, 0, data.height() - row, true); }
        } else {
            if (col < 0) { x = 0; y = row; clipped = truncate(value, -col, data.width(), false); }
            else { x = col; y = row; clipped = truncate(value, 0, data.width() - col, false); }
        }
        if (data.set(x, y, clipped, vertical)) onBufferSet(x, row, clipped, vertical);
    }

    @Override default char get(int col, int row) { return (char) data().get(col, row); }
    @Override default int getCodePoint(int col, int row) { return data().get(col, row); }
    @Override default int getForegroundColor(int col, int row) {
        int packed = color(col, row);
        int value = PackedColor.extractForeground(packed);
        return data().format().isFromPalette(value) ? value : PackedColor.unpackForeground(packed, data().format());
    }
    @Override default boolean isForegroundFromPalette(int col, int row) {
        return data().format().isFromPalette(PackedColor.extractForeground(color(col, row)));
    }
    @Override default int getBackgroundColor(int col, int row) {
        int packed = color(col, row);
        int value = PackedColor.extractBackground(packed);
        return data().format().isFromPalette(value) ? value : PackedColor.unpackBackground(packed, data().format());
    }
    @Override default boolean isBackgroundFromPalette(int col, int row) {
        return data().format().isFromPalette(PackedColor.extractBackground(color(col, row)));
    }

    @Override default void rawSetText(int col, int row, char[][] text) {
        for (int y = row; y < Math.min(row + text.length, data().height()); y++) {
            char[] line = text[y - row];
            for (int x = 0; x < Math.min(line.length, data().width()); x++) data().buffer[y][col + x] = line[x];
        }
    }
    @Override default void rawSetText(int col, int row, int[][] text) {
        for (int y = row; y < Math.min(row + text.length, data().height()); y++) {
            int[] line = text[y - row];
            System.arraycopy(line, 0, data().buffer[y], col, Math.min(line.length, data().width()));
        }
    }
    @Override default void rawSetForeground(int col, int row, int[][] values) {
        for (int y = row; y < Math.min(row + values.length, data().height()); y++) {
            int[] line = values[y - row];
            for (int x = col; x < Math.min(col + line.length, data().width()); x++) {
                int bg = data().color[y][x] & 0x00FF;
                int fg = (data().format().deflate(new PackedColorColor(line[x - col], false)) << 16) & 0xFF00;
                data().color[y][x] = fg | bg;
            }
        }
    }
    @Override default void rawSetBackground(int col, int row, int[][] values) {
        for (int y = row; y < Math.min(row + values.length, data().height()); y++) {
            int[] line = values[y - row];
            for (int x = col; x < Math.min(col + line.length, data().width()); x++) {
                int bg = data().format().deflate(new PackedColorColor(line[x - col], false)) & 0x00FF;
                int fg = data().color[y][x] & 0xFF00;
                data().color[y][x] = fg | bg;
            }
        }
    }

    private int color(int column, int row) {
        if (column < 0 || column >= getWidth() || row < 0 || row >= getHeight()) throw new IndexOutOfBoundsException();
        return data().color[row][column];
    }
}
