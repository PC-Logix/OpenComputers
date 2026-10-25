package li.cil.oc.common.datacomponents;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.phys.AABB;
import scala.Option;
import scala.collection.immutable.List$;
import scala.collection.immutable.Set$;
import scala.collection.mutable.Set;
import scala.jdk.javaapi.CollectionConverters;
import scala.jdk.javaapi.OptionConverters;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ScalaStreamCodecJava {
    public static final StreamCodec<ByteBuf, Boolean> BOOL = ByteBufCodecs.BOOL.map(value -> value, value -> value);
    public static final StreamCodec<ByteBuf, Integer> INT = ByteBufCodecs.INT.map(value -> value, value -> value);
    public static final StreamCodec<ByteBuf, int[]> INT_ARRAY = ByteBufCodecs.<ByteBuf, Integer>list().apply(INT).map(
        ScalaStreamCodecJava::toIntArray, ScalaStreamCodecJava::toIntegerList);
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = ByteBufCodecs.VAR_INT.map(value -> value, value -> value);
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = ByteBufCodecs.VAR_LONG.map(value -> value, value -> value);
    public static final StreamCodec<ByteBuf, Byte> BYTE = ByteBufCodecs.BYTE.map(value -> value, value -> value);
    public static final StreamCodec<ByteBuf, Float> FLOAT = ByteBufCodecs.FLOAT.map(value -> value, value -> value);
    public static final StreamCodec<ByteBuf, Double> DOUBLE = ByteBufCodecs.DOUBLE.map(value -> value, value -> value);

    public static final StreamCodec<ByteBuf, AABB> AABB_CODEC = StreamCodec.composite(
        DOUBLE, box -> box.minX,
        DOUBLE, box -> box.minY,
        DOUBLE, box -> box.minZ,
        DOUBLE, box -> box.maxX,
        DOUBLE, box -> box.maxY,
        DOUBLE, box -> box.maxZ,
        net.minecraft.world.phys.AABB::new);

    public static final StreamCodec<ByteBuf, ColorRGBA> COLOR_RGBA = ByteBufCodecs.INT.map(ColorRGBA::new, ColorRGBA::rgba);

    public static <B extends ByteBuf, T> StreamCodec<B, scala.collection.immutable.List<T>> list(StreamCodec<B, T> codec) {
        return ByteBufCodecs.<B, T>list().apply(codec).map(
            (List<T> decoded) -> List$.MODULE$.from(CollectionConverters.asScala(decoded)),
            (scala.collection.immutable.List<T> encoded) -> CollectionConverters.asJava(encoded));
    }

    public static <B extends ByteBuf, T> StreamCodec<B, Set<T>> mutableSet(StreamCodec<B, T> codec) {
        StreamCodec<B, List<T>> base = ByteBufCodecs.<B, T>list().apply(codec);
        Function<List<T>, Set<T>> decode = decoded -> scala.collection.mutable.Set$.MODULE$.from(CollectionConverters.asScala(decoded));
        Function<Set<T>, List<T>> encode = encoded -> {
            ArrayList<T> values = new ArrayList<>();
            scala.collection.Iterator<T> iterator = encoded.iterator();
            while (iterator.hasNext()) values.add(iterator.next());
            return values;
        };
        return base.map(decode, encode);
    }

    public static <B extends ByteBuf, T> StreamCodec<B, scala.collection.immutable.Set<T>> set(StreamCodec<B, T> codec) {
        return ByteBufCodecs.<B, T>list().apply(codec).map(
            (List<T> decoded) -> Set$.MODULE$.from(CollectionConverters.asScala(decoded)),
            (scala.collection.immutable.Set<T> encoded) -> CollectionConverters.asJava(encoded.toList()));
    }

    public static <B extends ByteBuf, T> StreamCodec<B, Option<T>> option(StreamCodec<B, T> codec) {
        return ByteBufCodecs.optional(codec).map(OptionConverters::toScala, OptionConverters::toJava);
    }

    public static <B extends ByteBuf, K, V> StreamCodec<B, scala.Tuple2<K, V>> pair(scala.Tuple2<StreamCodec<B, K>, StreamCodec<B, V>> codecs) {
        return StreamCodec.composite(codecs._1(), scala.Tuple2::_1, codecs._2(), scala.Tuple2::_2, scala.Tuple2::new);
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int index = 0; index < values.size(); index++) result[index] = values.get(index);
        return result;
    }

    private static List<Integer> toIntegerList(int[] values) {
        ArrayList<Integer> result = new ArrayList<>(values.length);
        for (int value : values) result.add(value);
        return result;
    }

    private ScalaStreamCodecJava() {}
}
