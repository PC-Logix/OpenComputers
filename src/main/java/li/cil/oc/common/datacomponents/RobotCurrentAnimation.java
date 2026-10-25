package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import scala.Option;

public record RobotCurrentAnimation(int ticksLeft,
                                    Option<BlockPos> moveFrom,
                                    boolean swingShovel,
                                    byte turnAxis) {
    public static final Codec<RobotCurrentAnimation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("ticks_left").forGetter(RobotCurrentAnimation::ticksLeft),
        ScalaCodec.optionFieldOf("move_from", BlockPos.CODEC, false).forGetter(RobotCurrentAnimation::moveFrom),
        Codec.BOOL.fieldOf("swing_shovel").forGetter(RobotCurrentAnimation::swingShovel),
        Codec.BYTE.fieldOf("turn_axis").forGetter(RobotCurrentAnimation::turnAxis)
    ).apply(instance, RobotCurrentAnimation::new));

    public static final StreamCodec<ByteBuf, RobotCurrentAnimation> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, RobotCurrentAnimation::ticksLeft,
        ScalaStreamCodec.option(BlockPos.STREAM_CODEC), RobotCurrentAnimation::moveFrom,
        ByteBufCodecs.BOOL, RobotCurrentAnimation::swingShovel,
        ByteBufCodecs.BYTE, RobotCurrentAnimation::turnAxis,
        RobotCurrentAnimation::new
    );
}
