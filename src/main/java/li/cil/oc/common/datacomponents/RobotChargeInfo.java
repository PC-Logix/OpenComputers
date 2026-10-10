package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RobotChargeInfo(int max, int have) {
    public static final Codec<RobotChargeInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("max").forGetter(RobotChargeInfo::max),
        Codec.INT.fieldOf("have").forGetter(RobotChargeInfo::have)
    ).apply(instance, RobotChargeInfo::new));

    public static final StreamCodec<ByteBuf, RobotChargeInfo> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, RobotChargeInfo::max,
        ByteBufCodecs.VAR_INT, RobotChargeInfo::have,
        RobotChargeInfo::new);
}
