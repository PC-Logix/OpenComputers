package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.codec.StreamCodec;
import org.junit.Test;
import scala.collection.immutable.List$;
import scala.jdk.javaapi.CollectionConverters;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ScalaCodecMigrationTest {
    @Test
    public void collectionDataCodecRoundTrips() {
        scala.collection.immutable.List<String> value = List$.MODULE$.from(CollectionConverters.asScala(List.of("first", "second")));
        Codec<scala.collection.immutable.List<String>> codec = ScalaCodec.list(Codec.STRING);

        var encoded = codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
        assertEquals(value, codec.parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    public void integerArrayStreamCodecRoundTrips() {
        @SuppressWarnings("unchecked")
        StreamCodec<ByteBuf, int[]> codec = (StreamCodec<ByteBuf, int[]>) (StreamCodec<?, ?>) ScalaStreamCodec.INT_ARRAY;
        int[] expected = {3, 1, 4, 1, 5};
        ByteBuf buffer = Unpooled.buffer();
        try {
            codec.encode(buffer, expected);
            assertArrayEquals(expected, codec.decode(buffer));
        } finally {
            buffer.release();
        }
    }

}
