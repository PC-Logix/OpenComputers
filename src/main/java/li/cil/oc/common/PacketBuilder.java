package li.cil.oc.common;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.SettingsAccessJava;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import scala.Option;
import scala.Option$;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

public abstract class PacketBuilder extends DataOutputStream {
    protected PacketBuilder(OutputStream stream) {
        super(stream);
    }

    public static java.io.ByteArrayOutputStream newData(boolean compressed) {
        java.io.ByteArrayOutputStream data = new java.io.ByteArrayOutputStream();
        data.write(compressed ? 1 : 0);
        return data;
    }

    public <T> void writeRegistryEntry(Registry<T> registry, T value) throws IOException {
        var key = registry.getKey(value);
        writeUTF(key == null ? "minecraft:air" : key.toString());
    }

    public void writeTileEntity(BlockEntity tile) throws IOException {
        writeUTF(tile.getLevel().dimension().location().toString());
        writeInt(tile.getBlockPos().getX());
        writeInt(tile.getBlockPos().getY());
        writeInt(tile.getBlockPos().getZ());
    }

    public void writeEntity(Entity entity) throws IOException {
        writeUTF(entity.level().dimension().location().toString());
        writeInt(entity.getId());
    }

    public void writeDirection(Option<Direction> direction) throws IOException {
        writeByte(direction.isDefined() ? direction.get().ordinal() : -1);
    }

    public void writeItemStack(ItemStack stack, RegistryAccess registryAccess) throws IOException {
        boolean haveStack = !stack.isEmpty() && stack.getCount() > 0;
        writeBoolean(haveStack);
        if (haveStack) writeNBT((CompoundTag) stack.save(registryAccess));
    }

    public void writeNBT(CompoundTag nbt) throws IOException {
        boolean haveNbt = nbt != null;
        writeBoolean(haveNbt);
        if (haveNbt) NbtIo.write(nbt, this);
    }

    public void writeMedium(int value) throws IOException {
        writeByte(value & 0xFF);
        writeByte((value >> 8) & 0xFF);
        writeByte((value >> 16) & 0xFF);
    }

    public void writeBlockPosCoords(BlockPosition position) throws IOException {
        writeInt(position.x());
        writeInt(position.y());
        writeInt(position.z());
    }

    public void writePacketType(PacketType type) throws IOException {
        writeByte(type.id());
    }

    public void sendToPlayersNearEntity(Entity entity) {
        sendToPlayersNearEntity(entity, Optional.empty());
    }

    public Option<Object> sendToPlayersNearEntity$default$2() { return Option$.MODULE$.empty(); }

    public void sendToPlayersNearEntity(Entity entity, Option<Object> range) {
        sendToPlayersNearEntity(entity, javaRange(range));
    }

    public void sendToPlayersNearEntity(Entity entity, Optional<?> range) {
        sendToNearbyPlayers(entity.level(), entity.getX(), entity.getY(), entity.getZ(), range);
    }

    public void sendToPlayersNearHost(EnvironmentHost host) {
        sendToPlayersNearHost(host, Optional.empty());
    }

    public Option<Object> sendToPlayersNearHost$default$2() { return Option$.MODULE$.empty(); }

    public void sendToPlayersNearHost(EnvironmentHost host, Option<Object> range) {
        sendToPlayersNearHost(host, javaRange(range));
    }

    public void sendToPlayersNearHost(EnvironmentHost host, Optional<?> range) {
        if (host instanceof BlockEntity tile) sendToPlayersNearTileEntity(tile, range);
        else sendToNearbyPlayers(host.getEnvironmentLevel(), host.xPosition(), host.yPosition(), host.zPosition(), range);
    }

    public void sendToPlayersNearTileEntity(BlockEntity tile) {
        sendToPlayersNearTileEntity(tile, Optional.empty());
    }

    public Option<Object> sendToPlayersNearTileEntity$default$2() { return Option$.MODULE$.empty(); }

    public void sendToPlayersNearTileEntity(BlockEntity tile, Option<Object> range) {
        sendToPlayersNearTileEntity(tile, javaRange(range));
    }

    public void sendToPlayersNearTileEntity(BlockEntity tile, Optional<?> range) {
        if (tile.getLevel() instanceof ServerLevel world) {
            ChunkPos chunk = new ChunkPos(tile.getBlockPos());
            var manager = ServerLifecycleHooks.getCurrentServer().getPlayerList();
            double maxRange = configuredRange(range, (manager.getViewDistance() + 1) * 16.0);
            double maxRangeSquared = maxRange * maxRange;
            world.getChunkSource().chunkMap.getPlayers(chunk, false).forEach(player -> {
                if (player.distanceToSqr(tile.getBlockPos().getX() + 0.5D, tile.getBlockPos().getY() + 0.5D,
                        tile.getBlockPos().getZ() + 0.5D) <= maxRangeSquared) sendToPlayer(player);
            });
        } else {
            sendToNearbyPlayers(tile.getLevel(), tile.getBlockPos().getX() + 0.5D, tile.getBlockPos().getY() + 0.5D,
                    tile.getBlockPos().getZ() + 0.5D, range);
        }
    }

    public void sendToNearbyPlayers(Level world, double x, double y, double z, Option<Object> range) {
        sendToNearbyPlayers(world, x, y, z, javaRange(range));
    }

    public void sendToNearbyPlayers(Level world, double x, double y, double z, Optional<?> range) {
        var manager = ServerLifecycleHooks.getCurrentServer().getPlayerList();
        double maxRange = configuredRange(range, (manager.getViewDistance() + 1) * 16.0);
        double maxRangeSquared = maxRange * maxRange;
        for (ServerPlayer player : manager.getPlayers()) {
            if (player.level() == world && player.distanceToSqr(x, y, z) <= maxRangeSquared) sendToPlayer(player);
        }
    }

    private static Optional<?> javaRange(Option<?> range) {
        return range.isDefined() ? Optional.of(range.get()) : Optional.empty();
    }

    private static double configuredRange(Optional<?> range, double defaultRange) {
        double value = range.isPresent() ? ((Number) range.get()).doubleValue() : defaultRange;
        double config = SettingsAccessJava.get().maxNetworkClientPacketDistance();
        return config > 0.0D ? Math.min(value, config) : value;
    }

    public void sendToAllPlayers() { PacketDistributor.sendToAllPlayers(new PacketPayload(packet())); }
    public void sendToPlayer(ServerPlayer player) { PacketDistributor.sendToPlayer(player, new PacketPayload(packet())); }
    public void sendToServer() { PacketDistributor.sendToServer(new PacketPayload(packet())); }

    protected abstract byte[] packet();
}
