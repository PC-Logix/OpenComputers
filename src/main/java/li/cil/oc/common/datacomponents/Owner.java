package li.cil.oc.common.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record Owner(String name, UUID id) {
    public static final Codec<Owner> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("name").forGetter(Owner::name),
        UUIDUtil.CODEC.fieldOf("id").forGetter(Owner::id)
    ).apply(instance, Owner::new));

    public static final StreamCodec<ByteBuf, Owner> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, Owner::name,
        UUIDUtil.STREAM_CODEC, Owner::id,
        Owner::new);
}
