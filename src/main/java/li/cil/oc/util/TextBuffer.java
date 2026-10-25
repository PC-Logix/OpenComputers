package li.cil.oc.util;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.Persistable;
import li.cil.oc.common.datacomponents.TextBufferContents;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Tuple2;

/**
 * Stores code points and packed colors in a two-dimensional buffer.
 * Updates use copy/fill operations to keep rendering and synchronization costs low.
 */
public class TextBuffer implements Persistable {
    private int width;
    private int height;
    private PackedColor.ColorFormat format;
    private PackedColorColor foreground = new PackedColorColor(0xFFFFFF, false);
    private PackedColorColor background = new PackedColorColor(0x000000, false);
    private int packed;
    public int[][] color;
    public int[][] buffer;

    public TextBuffer(int width, int height, PackedColor.ColorFormat initialFormat) {
        this.width = width;
        this.height = height;
        this.format = initialFormat;
        this.packed = PackedColor.pack(foreground, background, format);
        this.color = filled(height, width, packed);
        this.buffer = filled(height, width, 0x20);
    }

    public TextBuffer(Tuple2<?, ?> size, PackedColor.ColorFormat format) {
        this(((Number) size._1()).intValue(), ((Number) size._2()).intValue(), format);
    }

    private static int[][] filled(int height, int width, int value) {
        int[][] result = new int[height][width];
        for (int y = 0; y < height; y++) java.util.Arrays.fill(result[y], value);
        return result;
    }

    public int width() { return width; }
    public void width_$eq(int value) { width = value; }
    public int height() { return height; }
    public void height_$eq(int value) { height = value; }
    public PackedColorColor foreground() { return foreground; }
    public TextBuffer foreground_$eq(PackedColorColor value) {
        format.validate(value);
        foreground = value;
        packed = PackedColor.pack(foreground, background, format);
        return this;
    }
    public PackedColorColor background() { return background; }
    public TextBuffer background_$eq(PackedColorColor value) {
        format.validate(value);
        background = value;
        packed = PackedColor.pack(foreground, background, format);
        return this;
    }
    public PackedColor.ColorFormat format() { return format; }
    public boolean format_$eq(PackedColor.ColorFormat value) {
        if (format.depth() != value.depth()) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int old = color[y][x];
                    PackedColorColor fg = new PackedColorColor(PackedColor.unpackForeground(old, format), false);
                    PackedColorColor bg = new PackedColorColor(PackedColor.unpackBackground(old, format), false);
                    color[y][x] = PackedColor.pack(fg, bg, value);
                }
            }
            format = value;
            packed = PackedColor.pack(foreground, background, format);
            return true;
        }
        return false;
    }
    public Tuple2<Integer, Integer> size() { return new Tuple2<>(width, height); }
    public boolean size_$eq(Tuple2<Integer, Integer> value) {
        int newWidth = Math.max((Integer) value._1(), 1);
        int newHeight = Math.max((Integer) value._2(), 1);
        if (width == newWidth && height == newHeight) return false;
        int[][] newBuffer = filled(newHeight, newWidth, 0x20);
        int[][] newColor = filled(newHeight, newWidth, packed);
        for (int y = 0; y < Math.min(newHeight, height); y++) {
            System.arraycopy(buffer[y], 0, newBuffer[y], 0, Math.min(newWidth, width));
            System.arraycopy(color[y], 0, newColor[y], 0, Math.min(newWidth, width));
        }
        buffer = newBuffer;
        color = newColor;
        width = newWidth;
        height = newHeight;
        return true;
    }

    public int get(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) throw new IndexOutOfBoundsException();
        return buffer[row][col];
    }

    public boolean set(int col, int row, String value, boolean vertical) {
        if (vertical) {
            if (col < 0 || col >= width) return false;
            boolean changed = false;
            int y = row;
            for (int i = 0; i < value.length();) {
                int cp = value.codePointAt(i);
                if (FontUtils.wcwidth(cp) > 0) {
                    if (y >= 0 && y < height) {
                        changed |= buffer[y][col] != cp || color[y][col] != packed;
                        setChar(buffer[y], color[y], col, cp);
                    }
                    y++;
                }
                i += Character.charCount(cp);
                if (y >= height) break;
            }
            return changed;
        }
        if (row < 0 || row >= height) return false;
        boolean changed = false;
        int x = Math.max(col, 0);
        for (int i = 0; i < value.length() && x < width;) {
            int cp = value.codePointAt(i);
            int charWidth = FontUtils.wcwidth(cp);
            if (charWidth > 0) {
                changed |= buffer[row][x] != cp || color[row][x] != packed;
                setChar(buffer[row], color[row], x, cp);
                x += charWidth;
            }
            i += Character.charCount(cp);
        }
        return changed;
    }

    public boolean fill(int col, int row, int w, int h, int cp) {
        if (w <= 0 || h <= 0 || FontUtils.wcwidth(cp) == 0 || col + w < 0 || row + h < 0 || col >= width || row >= height) return false;
        boolean changed = false;
        int charWidth = FontUtils.wcwidth(cp);
        for (int y = Math.max(row, 0); y < Math.min(row + h, height); y++) {
            int x = Math.max(col, 0);
            while (x < Math.min(col + w, width)) {
                changed |= buffer[y][x] != cp || color[y][x] != packed;
                setChar(buffer[y], color[y], x, cp);
                x += charWidth;
            }
        }
        return changed;
    }

    public boolean copy(int col, int row, int w, int h, int tx, int ty) {
        if (w <= 0 || h <= 0 || (tx == 0 && ty == 0)) return false;
        int dx0 = Math.max(0, Math.min(width - 1, col + tx + w - 1));
        int dx1 = Math.max(0, Math.min(width, col + tx));
        if (tx <= 0) { int tmp = dx0; dx0 = dx1; dx1 = tmp; }
        int leftEdge = Math.min(dx0, dx1) - 1;
        if (leftEdge >= width - 1) return false;
        int dy0 = Math.max(0, Math.min(height - 1, row + ty + h - 1));
        int dy1 = Math.max(0, Math.min(height, row + ty));
        if (ty <= 0) { int tmp = dy0; dy0 = dy1; dy1 = tmp; }
        int sx = tx > 0 ? -1 : 1;
        int sy = ty > 0 ? -1 : 1;
        boolean changed = false;
        for (int y = dy0; sy > 0 ? y <= dy1 : y >= dy1; y += sy) {
            int sourceY = y - ty;
            if (sourceY < 0 || sourceY >= height) continue;
            for (int x = dx0; sx > 0 ? x <= dx1 : x >= dx1; x += sx) {
                int sourceX = x - tx;
                if (sourceX < 0 || sourceX >= width) continue;
                changed |= buffer[y][x] != buffer[sourceY][sourceX] || color[y][x] != color[sourceY][sourceX];
                buffer[y][x] = buffer[sourceY][sourceX];
                color[y][x] = color[sourceY][sourceX];
                for (int offset = 1; offset < FontUtils.wcwidth(buffer[y][x]); offset++) {
                    buffer[y][x + offset] = ' ';
                    color[y][x + offset] = color[sourceY][sourceX];
                }
            }
            if (leftEdge >= 0 && FontUtils.wcwidth(buffer[y][leftEdge]) > 1) {
                buffer[y][leftEdge] = ' ';
                changed = true;
            }
        }
        return changed;
    }

    public boolean rawcopy(int col, int row, int w, int h, TextBuffer src, int fromCol, int fromRow) {
        boolean changed = false;
        int x0 = col - 1, y0 = row - 1;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int ch = src.buffer[fromRow + y - 1][fromCol + x - 1];
            int colorValue = src.color[fromRow + y - 1][fromCol + x - 1];
            if (format.depth() != src.format.depth()) {
                colorValue = PackedColor.pack(
                        new PackedColorColor(PackedColor.unpackForeground(colorValue, src.format), false),
                        new PackedColorColor(PackedColor.unpackBackground(colorValue, src.format), false), format);
            }
            if (buffer[y0 + y][x0 + x] != ch || color[y0 + y][x0 + x] != colorValue) {
                changed = true;
                buffer[y0 + y][x0 + x] = ch;
                color[y0 + y][x0 + x] = colorValue;
            }
        }
        return changed;
    }

    private void setChar(int[] line, int[] lineColor, int x, int cp) {
        int charWidth = FontUtils.wcwidth(cp);
        if (charWidth > 1 && x >= line.length - 1) return;
        line[x] = cp;
        lineColor[x] = packed;
        for (int i = x + 1; i < x + charWidth; i++) { line[i] = ' '; lineColor[i] = packed; }
        if (x > 0 && FontUtils.wcwidth(line[x - 1]) > 1) line[x - 1] = ' ';
    }

    @Override public void loadData(DataComponentHolder holder) {
        var optional = ExtendedDataComponentHolderSupportJava.convert(holder)
                .getComponentOptional(OCComponentsJava.TEXT_BUFFER.get());
        if (optional.isEmpty()) return;
        TextBufferContents contents = optional.get();
        size_$eq(new Tuple2<>(contents.width(), contents.height()));
        var lines = contents.buffer();
        for (int i = 0; i < Math.min(height, lines.size()); i++) {
            String line = lines.get(i);
            int[] cps = line.codePoints().toArray();
            System.arraycopy(cps, 0, buffer[i], 0, Math.min(width, cps.length));
        }
        var depths = li.cil.oc.api.internal.TextBuffer.ColorDepth.values();
        int depthTag = Math.max(0, Math.min(contents.depth(), depths.length - 1));
        format = PackedColor.formatForDepth(depths[depthTag]);
        format.loadData(holder);
        foreground_$eq(new PackedColorColor(contents.foreground(), contents.foregroundIsPalette()));
        background_$eq(new PackedColorColor(contents.background(), contents.backgroundIsPalette()));
        color = contents.colors().sized(width, height);
    }

    @Override public void saveData(MutableDataComponentHolder holder) {
        java.util.List<String> lines = new java.util.ArrayList<>(height);
        for (int y = 0; y < height; y++) lines.add(lineToString(y));
        ExtendedDataComponentHolderSupportJava.convert(holder).setComponent(OCComponentsJava.TEXT_BUFFER.get(), new TextBufferContents(width, height, format.depth().ordinal(),
                foreground.value(), foreground.isPalette(), background.value(), background.isPalette(),
                lines,
                TextBufferContents.intArray(color)));
        format.saveData(holder);
    }

    public String lineToString(int y) {
        StringBuilder result = new StringBuilder();
        for (int x = 0; x < width; x++) result.appendCodePoint(buffer[y][x]);
        return result.toString();
    }

    @Override public String toString() {
        StringBuilder result = new StringBuilder();
        for (int y = 0; y < height; y++) {
            if (y > 0) result.append('\n');
            for (int x = 0; x < width; x++) result.appendCodePoint(buffer[y][x]);
        }
        return result.toString();
    }
}
