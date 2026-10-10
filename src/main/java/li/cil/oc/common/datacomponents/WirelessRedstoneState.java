package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record WirelessRedstoneState(int frequency, boolean input, boolean output) {
    public static final Codec<WirelessRedstoneState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("frequency").forGetter(WirelessRedstoneState::frequency),
        Codec.BOOL.fieldOf("input").forGetter(WirelessRedstoneState::input),
        Codec.BOOL.fieldOf("output").forGetter(WirelessRedstoneState::output)
    ).apply(instance, WirelessRedstoneState::new));
}
