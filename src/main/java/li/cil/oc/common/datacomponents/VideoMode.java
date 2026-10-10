package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record VideoMode(int width, int height) {
    public static final Codec<VideoMode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("width").forGetter(VideoMode::width),
        Codec.INT.fieldOf("height").forGetter(VideoMode::height)
    ).apply(instance, VideoMode::new));

    public static final StreamCodec<ByteBuf, VideoMode> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, VideoMode::width,
        ByteBufCodecs.VAR_INT, VideoMode::height,
        VideoMode::new);
}
