package li.cil.oc.common.item.data;

import net.minecraft.world.phys.AABB;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;

import java.util.Objects;
import java.util.Optional;

public final class PrintShape implements Comparable<PrintShape> {
    private final AABB bounds;
    private final String texture;
    private final Optional<Integer> tint;

    public PrintShape(AABB bounds, String texture, Optional<Integer> tint) {
        this.bounds = bounds;
        this.texture = texture;
        this.tint = tint;
    }

    /** Scala compatibility constructor; Java callers should use the Optional overload. */
    @Deprecated
    public PrintShape(AABB bounds, String texture, Option<?> tint) {
        this(bounds, texture, OptionConverters.toJava(tint).map(value -> (Integer) value));
    }

    public AABB bounds() { return bounds; }
    public String texture() { return texture; }

    public Optional<Integer> tintJava() { return tint; }

    /** Scala compatibility accessor; Java callers should use {@link #tintJava()}. */
    @Deprecated
    @SuppressWarnings("unchecked")
    public <T> Option<T> tint() { return (Option<T>) (Option<?>) OptionConverters.toScala(tint); }

    @Override
    public int compareTo(PrintShape that) {
        int order;
        if ((order = Double.compare(bounds.minX, that.bounds.minX)) != 0) return order;
        if ((order = Double.compare(bounds.minY, that.bounds.minY)) != 0) return order;
        if ((order = Double.compare(bounds.minZ, that.bounds.minZ)) != 0) return order;
        if ((order = Double.compare(bounds.maxX, that.bounds.maxX)) != 0) return order;
        if ((order = Double.compare(bounds.maxY, that.bounds.maxY)) != 0) return order;
        if ((order = Double.compare(bounds.maxZ, that.bounds.maxZ)) != 0) return order;
        if ((order = compareTint(tint, that.tint)) != 0) return order;
        return texture.compareTo(that.texture);
    }

    private static int compareTint(Optional<Integer> first, Optional<Integer> second) {
        if (first.isEmpty()) return second.isEmpty() ? 0 : -1;
        if (second.isEmpty()) return 1;
        return Integer.compare((Integer) first.get(), (Integer) second.get());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PrintShape that && bounds.equals(that.bounds)
            && texture.equals(that.texture) && Objects.equals(tint, that.tint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bounds, texture, tint);
    }
}
