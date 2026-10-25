package li.cil.oc.common.datacomponents;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.phys.AABB;
import scala.Option;
import scala.Tuple2;
import scala.collection.immutable.List;
import scala.collection.immutable.Set;

/** Stream codecs that preserve the Scala collection types used by data components. */
public final class ScalaStreamCodec {
    public static final StreamCodec<ByteBuf, Boolean> BOOL = ScalaStreamCodecJava.BOOL;
    public static final StreamCodec<ByteBuf, Integer> INT = ScalaStreamCodecJava.INT;
    public static final StreamCodec<ByteBuf, int[]> INT_ARRAY = ScalaStreamCodecJava.INT_ARRAY;
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = ScalaStreamCodecJava.VAR_INT;
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = ScalaStreamCodecJava.VAR_LONG;
    public static final StreamCodec<ByteBuf, Byte> BYTE = ScalaStreamCodecJava.BYTE;
    public static final StreamCodec<ByteBuf, Float> FLOAT = ScalaStreamCodecJava.FLOAT;
    public static final StreamCodec<ByteBuf, Double> DOUBLE = ScalaStreamCodecJava.DOUBLE;
    public static final StreamCodec<ByteBuf, AABB> AABB = ScalaStreamCodecJava.AABB_CODEC;
    public static final StreamCodec<ByteBuf, ColorRGBA> COLOR_RGBA = ScalaStreamCodecJava.COLOR_RGBA;

    public static <B extends ByteBuf, T> StreamCodec<B, List<T>> list(StreamCodec<B, T> codec) {
        return ScalaStreamCodecJava.list(codec);
    }
    public static <B extends ByteBuf, T> StreamCodec<B, scala.collection.mutable.Set<T>> mutableSet(StreamCodec<B, T> codec) {
        return ScalaStreamCodecJava.mutableSet(codec);
    }
    public static <B extends ByteBuf, T> StreamCodec<B, Set<T>> set(StreamCodec<B, T> codec) {
        return ScalaStreamCodecJava.set(codec);
    }
    public static <B extends ByteBuf, T> StreamCodec<B, Option<T>> option(StreamCodec<B, T> codec) {
        return ScalaStreamCodecJava.option(codec);
    }
    public static <B extends ByteBuf, A, C> StreamCodec<B, Tuple2<A, C>> pair(Tuple2<StreamCodec<B, A>, StreamCodec<B, C>> codecs) {
        return ScalaStreamCodecJava.pair(codecs);
    }

    private ScalaStreamCodec() {}
}
