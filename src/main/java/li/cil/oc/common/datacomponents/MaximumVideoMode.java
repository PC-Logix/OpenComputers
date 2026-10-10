package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MaximumVideoMode(int width, int height, int depth) {
    public static final Codec<MaximumVideoMode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("width").forGetter(MaximumVideoMode::width),
        Codec.INT.fieldOf("height").forGetter(MaximumVideoMode::height),
        Codec.INT.fieldOf("depth").forGetter(MaximumVideoMode::depth)
    ).apply(instance, MaximumVideoMode::new));

    public static final StreamCodec<ByteBuf, MaximumVideoMode> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MaximumVideoMode::width,
        ByteBufCodecs.VAR_INT, MaximumVideoMode::height,
        ByteBufCodecs.VAR_INT, MaximumVideoMode::depth,
        MaximumVideoMode::new);
}
