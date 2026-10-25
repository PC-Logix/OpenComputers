package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import scala.Option;
import scala.collection.immutable.List$;
import scala.collection.immutable.Map$;
import scala.collection.immutable.Set$;
import scala.collection.mutable.Set;
import scala.jdk.javaapi.CollectionConverters;
import scala.jdk.javaapi.OptionConverters;
import scala.math.Ordering;

import java.util.stream.IntStream;
import java.util.ArrayList;
import java.util.function.Function;

public final class ScalaCodecJava {
    public static final Codec<Boolean> BOOL = Codec.BOOL.xmap(value -> value, value -> value);
    public static final Codec<Integer> INT = Codec.INT.xmap(value -> value, value -> value);
    public static final Codec<int[]> INT_ARRAY = Codec.INT_STREAM.xmap(IntStream::toArray, IntStream::of);
    public static final Codec<Long> LONG = Codec.LONG.xmap(value -> value, value -> value);
    public static final Codec<Byte> BYTE = Codec.BYTE.xmap(value -> value, value -> value);
    public static final Codec<Float> FLOAT = Codec.FLOAT.xmap(value -> value, value -> value);
    public static final Codec<Double> DOUBLE = Codec.DOUBLE.xmap(value -> value, value -> value);

    public static final Codec<AABB> AABB_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Vec3.CODEC.fieldOf("from").forGetter(AABB::getMinPosition),
        Vec3.CODEC.fieldOf("to").forGetter(AABB::getMaxPosition)
    ).apply(instance, AABB::new));

    public static <K, V> Codec<scala.collection.immutable.Map<K, V>> map(Codec<K> key, Codec<V> value) {
        return Codec.unboundedMap(key, value).xmap(
            (java.util.Map<K, V> decoded) -> Map$.MODULE$.from(CollectionConverters.asScala(decoded)),
            (scala.collection.immutable.Map<K, V> encoded) -> CollectionConverters.asJava(encoded));
    }

    public static <T> Codec<scala.collection.immutable.List<T>> list(Codec<T> codec) {
        return Codec.list(codec).xmap(
            (java.util.List<T> decoded) -> List$.MODULE$.from(CollectionConverters.asScala(decoded)),
            (scala.collection.immutable.List<T> encoded) -> CollectionConverters.asJava(encoded));
    }

    public static <T> Codec<Set<T>> mutableSet(Codec<T> codec, Ordering<T> ordering) {
        Codec<java.util.List<T>> base = Codec.list(codec);
        Function<java.util.List<T>, Set<T>> decode = decoded -> scala.collection.mutable.Set$.MODULE$.from(CollectionConverters.asScala(decoded));
        Function<Set<T>, java.util.List<T>> encode = encoded -> {
            ArrayList<T> sorted = new ArrayList<>();
            scala.collection.Iterator<T> iterator = encoded.iterator();
            while (iterator.hasNext()) sorted.add(iterator.next());
            sorted.sort(ordering::compare);
            return sorted;
        };
        return base.xmap(decode, encode);
    }

    public static <T> Codec<scala.collection.immutable.Set<T>> set(Codec<T> codec) {
        return Codec.list(codec).xmap(
            (java.util.List<T> decoded) -> Set$.MODULE$.from(CollectionConverters.asScala(decoded)),
            (scala.collection.immutable.Set<T> encoded) -> CollectionConverters.asJava(encoded.toList()));
    }

    public static <T> MapCodec<Option<T>> optionFieldOf(String name, Codec<T> codec, boolean lenient) {
        return Codec.optionalField(name, codec, lenient).xmap(OptionConverters::toScala, OptionConverters::toJava);
    }

    public static <A, B> Codec<scala.Tuple2<A, B>> pair(scala.Tuple2<Codec<A>, Codec<B>> codecs) {
        return RecordCodecBuilder.create(instance -> instance.group(
            codecs._1().fieldOf("first").forGetter(scala.Tuple2::_1),
            codecs._2().fieldOf("second").forGetter(scala.Tuple2::_2)
        ).apply(instance, scala.Tuple2::new));
    }

    private ScalaCodecJava() {}
}
