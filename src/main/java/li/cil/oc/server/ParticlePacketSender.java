package li.cil.oc.server;

import li.cil.oc.common.PacketType;
import li.cil.oc.common.SimplePacketBuilder;
import li.cil.oc.util.BlockPosition;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import scala.Option;
import scala.Option$;

import java.io.IOException;
import java.util.Optional;

public final class ParticlePacketSender {
    private ParticlePacketSender() {}

    public static void send(BlockPosition position, ParticleOptions particleType, int count, double velocity,
                            Optional<Direction> direction, double range) throws IOException {
        if (count <= 0) return;
        Level world = position.worldOptional().orElseThrow();
        SimplePacketBuilder packet = new SimplePacketBuilder(PacketType.ParticleEffect);
        packet.writeUTF(world.dimension().location().toString());
        packet.writeInt(position.x());
        packet.writeInt(position.y());
        packet.writeInt(position.z());
        packet.writeDouble(velocity);
        packet.writeDirection(direction.isPresent() ? Option$.MODULE$.apply(direction.get()) : Option$.MODULE$.empty());
        packet.writeRegistryEntry(BuiltInRegistries.PARTICLE_TYPE, particleType.getType());
        packet.writeByte(count);
        packet.sendToNearbyPlayers(world, position.x(), position.y(), position.z(), Option$.MODULE$.apply(range));
    }

    public static void sendUnchecked(BlockPosition position, ParticleOptions particleType, int count, double velocity,
                                     Optional<Direction> direction, double range) {
        try {
            send(position, particleType, count, velocity, direction, range);
        } catch (IOException error) {
            ParticlePacketSender.<RuntimeException>throwUnchecked(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }
}
