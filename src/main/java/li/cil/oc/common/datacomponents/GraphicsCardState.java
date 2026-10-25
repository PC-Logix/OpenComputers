package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import scala.Option;

public record GraphicsCardState(Option<String> screen, int bufferIndex) {
    public static final Codec<GraphicsCardState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ScalaCodec.optionFieldOf("screen", Codec.STRING, false).forGetter(GraphicsCardState::screen),
        Codec.INT.fieldOf("bufferIndex").forGetter(GraphicsCardState::bufferIndex)
    ).apply(instance, GraphicsCardState::new));
}
