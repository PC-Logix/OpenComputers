package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record MFCoords(ResourceLocation dimension, BlockPos blockPos, Direction side) {
    public static final Codec<MFCoords> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("dimension").forGetter(MFCoords::dimension),
        BlockPos.CODEC.fieldOf("position").forGetter(MFCoords::blockPos),
        Direction.CODEC.fieldOf("side").forGetter(MFCoords::side)
    ).apply(instance, MFCoords::new));

    public static final StreamCodec<ByteBuf, MFCoords> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, MFCoords::dimension,
        BlockPos.STREAM_CODEC, MFCoords::blockPos,
        Direction.STREAM_CODEC, MFCoords::side,
        MFCoords::new);
}
