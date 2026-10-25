package li.cil.oc.common;

import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedLevelJava;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import scala.Option;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/** Shared binary parser for clientbound and serverbound OpenComputers packets. */
public abstract class PacketHandlerBase {
    /** Return the player's world for the requested dimension, if available. */
    protected abstract Optional<Level> world(Player player, ResourceLocation dimension);

    public abstract void dispatch(PacketParser parser);

    protected abstract PacketParser createParser(InputStream stream, Player player);

    public static class PacketParser extends DataInputStream {
        private final PacketHandlerBase handler;
        public final Player player;
        public final PacketType packetType;

        public PacketParser(InputStream stream, Player player, PacketHandlerBase handler) throws IOException {
            super(stream);
            this.handler = handler;
            this.player = player;
            packetType = PacketType.fromId(readByte());
        }

        public <T> T readRegistryEntry(Registry<T> registry) throws IOException {
            ResourceLocation location = ResourceLocation.tryParse(readUTF());
            return registry.get(location != null ? location : ResourceLocation.parse("minecraft:air"));
        }

        public <T> Optional<T> getBlockEntityOptional(ResourceLocation dimension, int x, int y, int z, Class<T> type) {
            Optional<Level> selectedWorld = handler.world(player, dimension);
            if (selectedWorld.isEmpty()) return Optional.empty();

            Level level = selectedWorld.get();
            BlockPosition position = BlockPositionsJava.apply(x, y, z);
            ExtendedLevelJava access = new ExtendedLevelJava(level);
            if (!access.blockExists(position)) return Optional.empty();

            Object blockEntity = access.getBlockEntity(position);
            if (type.isInstance(blockEntity)) return Optional.of(type.cast(blockEntity));

            // A robot can move away between the packet being sent and parsed.
            var movingRobot = OCBlocks.RobotAfterimage.get().findMovingRobotOptional(level, new BlockPos(x, y, z));
            if (movingRobot.isPresent() && type.isInstance(movingRobot.get().proxy()))
                return Optional.of(type.cast(movingRobot.get().proxy()));
            return Optional.empty();
        }

        public <T> Option<T> getBlockEntity(ResourceLocation dimension, int x, int y, int z, Class<T> type) {
            return Option.apply(getBlockEntityOptional(dimension, x, y, z, type).orElse(null));
        }

        public <T> Optional<T> getEntityOptional(ResourceLocation dimension, int id, Class<T> type) {
            Optional<Level> selectedWorld = handler.world(player, dimension);
            if (selectedWorld.isEmpty()) return Optional.empty();
            Object entity = selectedWorld.get().getEntity(id);
            return type.isInstance(entity) ? Optional.of(type.cast(entity)) : Optional.empty();
        }

        public <T> Option<T> getEntity(ResourceLocation dimension, int id, Class<T> type) {
            return Option.apply(getEntityOptional(dimension, id, type).orElse(null));
        }

        public <T> Optional<T> readBlockEntityOptional(Class<T> type) throws IOException {
            ResourceLocation dimension = ResourceLocation.tryParse(readUTF());
            int x = readInt();
            int y = readInt();
            int z = readInt();
            return getBlockEntityOptional(dimension, x, y, z, type);
        }

        public <T> Option<T> readBlockEntity(Class<T> type) throws IOException {
            return Option.apply(readBlockEntityOptional(type).orElse(null));
        }

        public <T> Optional<T> readEntityOptional(Class<T> type) throws IOException {
            ResourceLocation dimension = ResourceLocation.tryParse(readUTF());
            return getEntityOptional(dimension, readInt(), type);
        }

        public <T> Option<T> readEntity(Class<T> type) throws IOException {
            return Option.apply(readEntityOptional(type).orElse(null));
        }

        public Optional<Direction> readDirectionOptional() throws IOException {
            int id = readByte();
            return id < 0 ? Optional.empty() : Optional.of(Direction.from3DDataValue(id));
        }

        public Option<Direction> readDirection() throws IOException {
            return Option.apply(readDirectionOptional().orElse(null));
        }

        public ItemStack readItemStack() throws IOException {
            return readBoolean() ? ItemStack.parseOptional(player.level().registryAccess(), readNBT()) : ItemStack.EMPTY;
        }

        public CompoundTag readNBT() throws IOException {
            return readBoolean() ? NbtIo.read(this) : null;
        }

        public int readMedium() throws IOException {
            int c0 = readUnsignedByte();
            int c1 = readUnsignedByte();
            int c2 = readUnsignedByte();
            return c0 | (c1 << 8) | (c2 << 16);
        }

        public BlockPosition readBlockPosCoords() throws IOException {
            return new BlockPosition(readInt(), readInt(), readInt());
        }

        public byte[] readByteArray() throws IOException {
            byte[] value = new byte[readInt()];
            readFully(value);
            return value;
        }

        public PacketType readPacketType() throws IOException {
            return PacketType.fromId(readByte());
        }
    }
}
