package li.cil.oc.client;

import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.event.NetworkActivityEvent;
import li.cil.oc.common.PacketHandlerBase.PacketParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

/** Dispatches clientbound file-system and network activity events. */
public final class ClientActivityPacketHandlers {
    private ClientActivityPacketHandlers() {}

    public static void fileSystemActivity(PacketParser packet, Function<ResourceLocation, Optional<Level>> worldLookup) throws IOException {
        String sound = packet.readUTF();
        CompoundTag data = NbtIo.read(packet);
        if (packet.readBoolean()) {
            Optional<BlockEntity> blockEntity = packet.readBlockEntityOptional(BlockEntity.class);
            blockEntity.ifPresent(value -> NeoForge.EVENT_BUS.post(new FileSystemAccessEvent.Client(sound, value, data)));
        } else {
            Optional<Level> world = worldLookup.apply(ResourceLocation.tryParse(packet.readUTF()));
            if (world.isPresent()) {
                double x = packet.readDouble();
                double y = packet.readDouble();
                double z = packet.readDouble();
                NeoForge.EVENT_BUS.post(new FileSystemAccessEvent.Client(sound, world.get(), x, y, z, data));
            }
        }
    }

    public static void networkActivity(PacketParser packet, Function<ResourceLocation, Optional<Level>> worldLookup) throws IOException {
        CompoundTag data = NbtIo.read(packet);
        if (packet.readBoolean()) {
            Optional<BlockEntity> blockEntity = packet.readBlockEntityOptional(BlockEntity.class);
            blockEntity.ifPresent(value -> NeoForge.EVENT_BUS.post(new NetworkActivityEvent.Client(value, data)));
        } else {
            Optional<Level> world = worldLookup.apply(ResourceLocation.tryParse(packet.readUTF()));
            if (world.isPresent()) {
                double x = packet.readDouble();
                double y = packet.readDouble();
                double z = packet.readDouble();
                NeoForge.EVENT_BUS.post(new NetworkActivityEvent.Client(world.get(), x, y, z, data));
            }
        }
    }
}
