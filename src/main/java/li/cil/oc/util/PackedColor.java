package li.cil.oc.util;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Settings;
import li.cil.oc.api.Persistable;
import li.cil.oc.api.internal.TextBuffer;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.List;

public final class PackedColor {
    private PackedColor() { }

    public static ColorFormat formatForDepth(TextBuffer.ColorDepth depth) {
        return Depth.format(depth);
    }

    public static final class Depth {
        private Depth() { }

        public static int bits(TextBuffer.ColorDepth depth) {
            return switch (depth) {
                case OneBit -> 1;
                case FourBit -> 4;
                case EightBit -> 8;
                case SixteenBit -> 16;
            };
        }

        public static ColorFormat format(TextBuffer.ColorDepth depth) {
            return switch (depth) {
                case OneBit -> new SingleBitFormat(Settings.get().monochromeColor());
                case FourBit -> new MutablePaletteFormat();
                case EightBit -> new HybridFormat();
                case SixteenBit -> new SixteenBitFormat();
            };
        }
    }

    private static final int R_SHIFT = 16;
    private static final int G_SHIFT = 8;
    private static final int B_SHIFT = 0;

    private static int[] extract(int value) {
        return new int[]{(value >>> R_SHIFT) & 0xFF, (value >>> G_SHIFT) & 0xFF, value & 0xFF};
    }

    public interface ColorFormat extends Persistable {
        TextBuffer.ColorDepth depth();
        int inflate(int value);
        short deflate(PackedColorColor value);
        default void validate(PackedColorColor value) {
            if (value.isPalette()) throw new IllegalArgumentException("color palette not supported");
        }
        default boolean isFromPalette(int value) { return false; }
        @Override default void loadData(DataComponentHolder holder) { }
        @Override default void saveData(MutableDataComponentHolder holder) { }
    }

    public static class SingleBitFormat implements ColorFormat {
        private final int color;

        public SingleBitFormat(int color) { this.color = color; }
        @Override public TextBuffer.ColorDepth depth() { return TextBuffer.ColorDepth.OneBit; }
        @Override public int inflate(int value) { return value == 0 ? 0x000000 : color; }
        @Override public short deflate(PackedColorColor value) { return (short) (value.value() == 0 ? 0 : 1); }
    }

    public abstract static class PaletteFormat implements ColorFormat {
        protected abstract int[] palette();

        @Override public int inflate(int value) {
            int[] colors = palette();
            return colors[Math.max(0, Math.min(colors.length - 1, value))];
        }

        @Override public void validate(PackedColorColor value) {
            if (value.isPalette() && (value.value() < 0 || value.value() >= palette().length)) {
                throw new IllegalArgumentException("invalid palette index");
            }
        }

        @Override public short deflate(PackedColorColor value) {
            int[] colors = palette();
            if (value.isPalette()) return (short) (Math.max(0, value.value()) % colors.length);
            int best = 0;
            double bestDelta = Double.POSITIVE_INFINITY;
            for (int i = 0; i < colors.length; i++) {
                double current = delta(value.value(), colors[i]);
                if (current < bestDelta) { best = i; bestDelta = current; }
            }
            return (short) best;
        }

        @Override public boolean isFromPalette(int value) { return true; }

        protected double delta(int colorA, int colorB) {
            int[] a = extract(colorA);
            int[] b = extract(colorB);
            int dr = a[0] - b[0], dg = a[1] - b[1], db = a[2] - b[2];
            return 0.2126 * dr * dr + 0.7152 * dg * dg + 0.0722 * db * db;
        }
    }

    public static class MutablePaletteFormat extends PaletteFormat {
        private final int[] palette = {
                0xFFFFFF, 0xFFCC33, 0xCC66CC, 0x6699FF,
                0xFFFF33, 0x33CC33, 0xFF6699, 0x333333,
                0xCCCCCC, 0x336699, 0x9933CC, 0x333399,
                0x663300, 0x336600, 0xFF3333, 0x000000};

        @Override public TextBuffer.ColorDepth depth() { return TextBuffer.ColorDepth.FourBit; }
        @Override protected int[] palette() { return palette; }
        public int apply(int index) { return palette[index]; }
        public void update(int index, int value) { palette[index] = value; }

        @Override public void loadData(DataComponentHolder holder) {
            super.loadData(holder);
            @SuppressWarnings("unchecked")
            scala.collection.immutable.List<Integer> colors = (scala.collection.immutable.List<Integer>) holder.get((DataComponentType) OCComponentsJava.PALETTE.get());
            if (colors != null) for (int i = 0; i < Math.min(colors.size(), palette.length); i++) palette[i] = colors.apply(i);
        }

        @Override public void saveData(MutableDataComponentHolder holder) {
            super.saveData(holder);
            scala.collection.immutable.List<Integer> colors = scala.jdk.javaapi.CollectionConverters.asScala(java.util.Arrays.stream(palette).boxed().toList()).toList();
            holder.set((DataComponentType) OCComponentsJava.PALETTE.get(), colors);
        }
    }

    public static class HybridFormat extends MutablePaletteFormat {
        private static final int REDS = 6, GREENS = 8, BLUES = 5;
        private final int[] staticPalette = new int[240];

        public HybridFormat() {
            for (int index = 0; index < staticPalette.length; index++) {
                int idxB = index % BLUES;
                int idxG = index / BLUES % GREENS;
                int idxR = index / BLUES / GREENS % REDS;
                int r = (int) (idxR * 0xFF / (REDS - 1.0) + 0.5);
                int g = (int) (idxG * 0xFF / (GREENS - 1.0) + 0.5);
                int b = (int) (idxB * 0xFF / (BLUES - 1.0) + 0.5);
                staticPalette[index] = (r << R_SHIFT) | (g << G_SHIFT) | (b << B_SHIFT);
            }
            for (int i = 0; i < palette().length; i++) {
                int shade = 0xFF * (i + 1) / (palette().length + 1);
                update(i, (shade << R_SHIFT) | (shade << G_SHIFT) | shade);
            }
        }

        @Override public TextBuffer.ColorDepth depth() { return TextBuffer.ColorDepth.EightBit; }
        @Override public int inflate(int value) {
            return isFromPalette(value) ? super.inflate(value) : staticPalette[(value - palette().length) % 240];
        }
        @Override public short deflate(PackedColorColor value) {
            short paletteIndex = super.deflate(value);
            if (value.isPalette()) return paletteIndex;
            int[] rgb = extract(value.value());
            int idxR = (int) (rgb[0] * (REDS - 1.0) / 0xFF + 0.5);
            int idxG = (int) (rgb[1] * (GREENS - 1.0) / 0xFF + 0.5);
            int idxB = (int) (rgb[2] * (BLUES - 1.0) / 0xFF + 0.5);
            short deflated = (short) (palette().length + idxR * GREENS * BLUES + idxG * BLUES + idxB);
            return delta(inflate(deflated & 0xFF), value.value()) < delta(inflate(paletteIndex & 0xFF), value.value())
                    ? deflated : paletteIndex;
        }
        @Override public boolean isFromPalette(int value) { return value >= 0 && value < palette().length; }
    }

    public static class SixteenBitFormat extends MutablePaletteFormat {
        private static final int R_BITS = 5, G_BITS = 6, B_BITS = 5;
        @Override public TextBuffer.ColorDepth depth() { return TextBuffer.ColorDepth.SixteenBit; }
        private int toRGB565(int value) {
            int[] rgb = extract(value);
            int r = (int) (rgb[0] * ((1 << R_BITS) - 1.0) / 0xFF + 0.5);
            int g = (int) (rgb[1] * ((1 << G_BITS) - 1.0) / 0xFF + 0.5);
            int b = (int) (rgb[2] * ((1 << B_BITS) - 1.0) / 0xFF + 0.5);
            return (r << (G_BITS + B_BITS)) | (g << B_BITS) | b;
        }
        private int fromRGB565(int value) {
            int r5 = value >>> (G_BITS + B_BITS) & 0x1F;
            int g6 = value >>> B_BITS & 0x3F;
            int b5 = value & 0x1F;
            int r = (int) (r5 * 0xFF / ((1 << R_BITS) - 1.0) + 0.5);
            int g = (int) (g6 * 0xFF / ((1 << G_BITS) - 1.0) + 0.5);
            int b = (int) (b5 * 0xFF / ((1 << B_BITS) - 1.0) + 0.5);
            return (r << R_SHIFT) | (g << G_SHIFT) | b;
        }
        @Override public int inflate(int value) {
            return isFromPalette(value) ? super.inflate(value) : fromRGB565(value - palette().length);
        }
        @Override public short deflate(PackedColorColor value) {
            short paletteIndex = super.deflate(value);
            if (value.isPalette()) return paletteIndex;
            short deflated = (short) (palette().length + toRGB565(value.value()));
            return delta(inflate(deflated & 0xFFFF), value.value()) < delta(inflate(paletteIndex & 0xFFFF), value.value())
                    ? deflated : paletteIndex;
        }
        @Override public boolean isFromPalette(int value) { return value >= 0 && value < palette().length; }
    }

    public static final int ForegroundShift = 16;
    public static final int BackgroundMask = 0x0000FFFF;

    public static int pack(PackedColorColor foreground, PackedColorColor background, ColorFormat format) {
        return ((format.deflate(foreground) & 0xFFFF) << ForegroundShift) | (format.deflate(background) & 0xFFFF);
    }
    public static int extractForeground(int color) { return color >>> ForegroundShift; }
    public static int extractBackground(int color) { return color & BackgroundMask; }
    public static int unpackForeground(int color, ColorFormat format) { return format.inflate(extractForeground(color)); }
    public static int unpackBackground(int color, ColorFormat format) { return format.inflate(extractBackground(color)); }
}
