package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.StreamCodec;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;

public record TextBufferContents(int width, int height, int depth,
                                 int foreground, boolean foregroundIsPalette,
                                 int background, boolean backgroundIsPalette,
                                 List<String> buffer, IntArray colors) {
    public TextBufferContents {
        buffer = List.copyOf(buffer);
    }

    public static IntArray intArray(int[][] values) {
        return new IntArray(values);
    }

    public static final Codec<TextBufferContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("width").forGetter(TextBufferContents::width),
        Codec.INT.fieldOf("height").forGetter(TextBufferContents::height),
        Codec.INT.fieldOf("depth").forGetter(TextBufferContents::depth),
        Codec.INT.fieldOf("foreground").forGetter(TextBufferContents::foreground),
        Codec.BOOL.fieldOf("foregroundIsPalette").forGetter(TextBufferContents::foregroundIsPalette),
        Codec.INT.fieldOf("background").forGetter(TextBufferContents::background),
        Codec.BOOL.fieldOf("backgroundIsPalette").forGetter(TextBufferContents::backgroundIsPalette),
        Codec.STRING.listOf().fieldOf("buffer").forGetter(TextBufferContents::buffer),
        IntArray.CODEC.fieldOf("colors").forGetter(TextBufferContents::colors)
    ).apply(instance, TextBufferContents::new));

    public static final class IntArray {
        public static final Codec<IntArray> CODEC = Codec.BYTE_BUFFER.xmap(IntArray::new, IntArray::intoByteBuffer);
        private final int[] array;

        public IntArray(int[] values) {
            array = values.clone();
        }

        public IntArray(int[][] values) {
            this(Arrays.stream(values).flatMapToInt(Arrays::stream).toArray());
        }

        public IntArray(IntBuffer buffer) {
            this(read(buffer));
        }

        public IntArray(ByteBuffer buffer) {
            this(buffer.asIntBuffer());
        }

        private static int[] read(IntBuffer buffer) {
            int[] values = new int[buffer.remaining()];
            buffer.get(values);
            return values;
        }

        public ByteBuffer intoByteBuffer() {
            ByteBuffer bytes = ByteBuffer.allocate(array.length * Integer.BYTES);
            bytes.asIntBuffer().put(array);
            return bytes;
        }

        public int[][] sized(int width, int height) {
            int[][] result = new int[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    result[y][x] = index < array.length ? array[index] : 0;
                }
            }
            return result;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof IntArray that && Arrays.equals(array, that.array);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(array);
        }
    }
}
