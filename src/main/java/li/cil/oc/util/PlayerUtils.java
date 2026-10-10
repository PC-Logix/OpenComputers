package li.cil.oc.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class PlayerUtils {
    private PlayerUtils() {
    }

    public static CompoundTag persistedData(Player player) {
        CompoundTag nbt = player.getPersistentData();
        if (!nbt.contains(Player.PERSISTED_NBT_TAG)) {
            nbt.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return nbt.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static void spawnParticleAround(Player player, ParticleOptions effectType) {
        spawnParticleAround(player, effectType, 1.0);
    }

    public static void spawnParticleAround(Player player, ParticleOptions effectType, double chance) {
        var rng = player.level().random;
        if (chance >= 1 || rng.nextDouble() < chance) {
            var bounds = player.getBoundingBox();
            double x = bounds.minX + (bounds.maxX - bounds.minX) * rng.nextDouble() * 1.5;
            double y = bounds.minY + (bounds.maxY - bounds.minY) * rng.nextDouble() * 0.5;
            double z = bounds.minZ + (bounds.maxZ - bounds.minZ) * rng.nextDouble() * 1.5;
            player.level().addParticle(effectType, x, y, z, 0, 0, 0);
        }
    }
}
