package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.phys.AABB;
import scala.Option;
import scala.Tuple2;
import scala.collection.immutable.List;
import scala.collection.immutable.Map;
import scala.collection.immutable.Set;

import scala.math.Ordering;

/** Scala collection codecs backed by the shared Java codec implementations. */
public final class ScalaCodec {
    public static final Codec<Boolean> BOOL = ScalaCodecJava.BOOL;
    public static final Codec<Integer> INT = ScalaCodecJava.INT;
    public static final Codec<int[]> INT_ARRAY = ScalaCodecJava.INT_ARRAY;
    public static final Codec<Long> LONG = ScalaCodecJava.LONG;
    public static final Codec<Byte> BYTE = ScalaCodecJava.BYTE;
    public static final Codec<Float> FLOAT = ScalaCodecJava.FLOAT;
    public static final Codec<Double> DOUBLE = ScalaCodecJava.DOUBLE;
    public static final Codec<AABB> AABB = ScalaCodecJava.AABB_CODEC;

    public static <K, V> Codec<Map<K, V>> map(Codec<K> key, Codec<V> value) {
        return ScalaCodecJava.map(key, value);
    }

    public static <T> Codec<List<T>> list(Codec<T> codec) {
        return ScalaCodecJava.list(codec);
    }

    public static <T> Codec<scala.collection.mutable.Set<T>> mutableSet(Codec<T> codec, Ordering<T> ordering) {
        return ScalaCodecJava.mutableSet(codec, ordering);
    }

    public static <T> Codec<Set<T>> set(Codec<T> codec) {
        return ScalaCodecJava.set(codec);
    }

    @SuppressWarnings("unchecked")
    public static <T> MapCodec<Option<T>> optionFieldOf(String name, Codec<T> codec, boolean lenient) {
        return (MapCodec<Option<T>>) (MapCodec<?>) ScalaCodecJava.optionFieldOf(name, codec, lenient);
    }

    public static <T> MapCodec<Option<T>> optionFieldOf(String name, Codec<T> codec) {
        return optionFieldOf(name, codec, false);
    }

    public static <A, B> Codec<Tuple2<A, B>> pair(Tuple2<Codec<A>, Codec<B>> codecs) {
        return ScalaCodecJava.pair(codecs);
    }

    private ScalaCodec() {}
}
