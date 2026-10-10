package li.cil.oc.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class PacketPayload implements CustomPacketPayload {
    public static final Type<PacketPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("opencomputers", "packet"));

    public static final StreamCodec<ByteBuf, PacketPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeInt(payload.data.length);
            buffer.writeBytes(payload.data);
        },
        buffer -> {
            int length = buffer.readInt();
            byte[] bytes = new byte[length];
            buffer.readBytes(bytes);
            return new PacketPayload(bytes);
        });

    public final byte[] data;

    public PacketPayload(byte[] data) {
        this.data = data;
    }

    @Override
    public Type<PacketPayload> type() {
        return TYPE;
    }
}
