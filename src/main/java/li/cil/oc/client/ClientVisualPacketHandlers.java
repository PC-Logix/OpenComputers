package li.cil.oc.client;

import li.cil.oc.Settings;
import li.cil.oc.client.renderer.PetRenderer;
import li.cil.oc.common.PacketHandlerBase.PacketParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.level.Level;
import java.io.IOException;
import java.util.Optional;
import java.util.function.ToIntFunction;

/** Applies clientbound visual effects and visibility state. */
public final class ClientVisualPacketHandlers {
    private ClientVisualPacketHandlers() {}

    public static void particleEffect(PacketParser packet, Optional<Level> selectedWorld) throws IOException {
        if (selectedWorld.isEmpty()) return;

        Level world = selectedWorld.get();
        int x = packet.readInt();
        int y = packet.readInt();
        int z = packet.readInt();
        double velocity = packet.readDouble();
        Optional<Direction> direction = packet.readDirectionOptional();
        Registry<ParticleType<?>> particleRegistry = packet.player.level().registryAccess().registryOrThrow(Registries.PARTICLE_TYPE);
        Object particleType = packet.readRegistryEntry(particleRegistry);
        if (!(particleType instanceof ParticleOptions particle)) return;

        int count = packet.readUnsignedByte() / (1 << Minecraft.getInstance().options.particles().get().getId());
        for (int index = 0; index < count; index++) {
            double vx = randomVelocity(world, direction, Direction::getStepX);
            double vy = randomVelocity(world, direction, Direction::getStepY);
            double vz = randomVelocity(world, direction, Direction::getStepZ);
            if (vx * vx + vy * vy + vz * vz < 1) {
                double px = particlePosition(x, vx, velocity, direction, Direction::getStepX);
                double py = particlePosition(y, vy, velocity, direction, Direction::getStepY);
                double pz = particlePosition(z, vz, velocity, direction, Direction::getStepZ);
                world.addParticle(particle, px, py, pz, vx, vy + velocity * 0.25, vz);
            }
        }
    }

    public static void petVisibility(PacketParser packet) throws IOException {
        if (!PetRenderer.isInitialized) {
            PetRenderer.isInitialized = true;
            if (Settings.get().hideOwnPet()) {
                PetRenderer.hidden.add(Minecraft.getInstance().player.getName().getString());
            }
            PacketSender.sendPetVisibility();
        }

        int count = packet.readInt();
        for (int index = 0; index < count; index++) {
            String name = packet.readUTF();
            if (packet.readBoolean()) PetRenderer.hidden.remove(name);
            else PetRenderer.hidden.add(name);
        }
    }

    private static double randomVelocity(Level world, Optional<Direction> direction, ToIntFunction<Direction> step) {
        float random = world.random.nextFloat();
        return direction.isPresent()
            ? random - 0.5 + step.applyAsInt(direction.get()) * 0.5
            : random * 2.0 - 1;
    }

    private static double particlePosition(int position, double value, double velocity,
                                           Optional<Direction> direction, ToIntFunction<Direction> step) {
        return direction.isPresent()
            ? position + 0.5 + value * velocity * 0.5 + step.applyAsInt(direction.get()) * velocity
            : position + 0.5 + value * velocity;
    }
}
