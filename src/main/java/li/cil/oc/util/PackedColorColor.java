package li.cil.oc.util;

/** A packed color value and whether it represents a palette index. */
public record PackedColorColor(int value, boolean isPalette) {
    public PackedColorColor(int value) {
        this(value, false);
    }
}
