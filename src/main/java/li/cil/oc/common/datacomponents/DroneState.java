package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DroneState(float targetX, float targetY, float targetZ, float targetAcceleration,
                         byte selectedSlot, byte selectedTank) {
    public static final Codec<DroneState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.FLOAT.fieldOf("targetX").forGetter(DroneState::targetX),
        Codec.FLOAT.fieldOf("targetY").forGetter(DroneState::targetY),
        Codec.FLOAT.fieldOf("targetZ").forGetter(DroneState::targetZ),
        Codec.FLOAT.fieldOf("targetAcceleration").forGetter(DroneState::targetAcceleration),
        Codec.BYTE.fieldOf("selectedSlot").forGetter(DroneState::selectedSlot),
        Codec.BYTE.fieldOf("selectedTank").forGetter(DroneState::selectedTank)
    ).apply(instance, DroneState::new));

    public static final StreamCodec<ByteBuf, DroneState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, DroneState::targetX,
        ByteBufCodecs.FLOAT, DroneState::targetY,
        ByteBufCodecs.FLOAT, DroneState::targetZ,
        ByteBufCodecs.FLOAT, DroneState::targetAcceleration,
        ByteBufCodecs.BYTE, DroneState::selectedSlot,
        ByteBufCodecs.BYTE, DroneState::selectedTank,
        DroneState::new);
}
