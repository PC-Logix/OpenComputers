package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.prefab.AbstractBehavior;
import li.cil.oc.api.prefab.AbstractProvider;
import li.cil.oc.util.PlayerUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class ParticleProvider extends AbstractProvider {
    public static final ParticleProvider INSTANCE = new ParticleProvider();
    public static final SimpleParticleType[] PARTICLE_TYPES = {
            ParticleTypes.FIREWORK, ParticleTypes.SMOKE, ParticleTypes.WITCH,
            ParticleTypes.NOTE, ParticleTypes.ENCHANT, ParticleTypes.FLAME,
            ParticleTypes.LAVA, ParticleTypes.SPLASH, ParticleTypes.ITEM_SLIME,
            ParticleTypes.HEART, ParticleTypes.HAPPY_VILLAGER
    };

    private ParticleProvider() { super("b48c4bbd-51bb-4915-9367-16cff3220e4b"); }

    @Override
    public Iterable<Behavior> createBehaviors(Player player) {
        List<Behavior> behaviors = new ArrayList<>(PARTICLE_TYPES.length);
        for (SimpleParticleType type : PARTICLE_TYPES) behaviors.add(new ParticleBehavior(type, player));
        return behaviors;
    }

    @Override
    protected void writeBehaviorToNBT(Behavior behavior, CompoundTag nbt) {
        if (behavior instanceof ParticleBehavior particles) {
            nbt.putInt("effectName", BuiltInRegistries.PARTICLE_TYPE.getId(particles.effectType));
        }
    }

    @Override
    protected Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        var type = BuiltInRegistries.PARTICLE_TYPE.getHolder(nbt.getInt("effectName")).orElseThrow().value();
        return new ParticleBehavior((SimpleParticleType) type, player);
    }

    public static final class ParticleBehavior extends AbstractBehavior {
        public SimpleParticleType effectType;

        public ParticleBehavior(SimpleParticleType effectType, Player player) {
            super(player);
            this.effectType = effectType;
        }

        @Override
        public String getNameHint() {
            return "particles." + BuiltInRegistries.PARTICLE_TYPE.getKey(effectType).getPath();
        }

        @Override
        public void update() {
            if (player.level().isClientSide() && Settings.get().enableNanomachinePfx()) {
                PlayerUtils.spawnParticleAround(player, effectType,
                        Nanomachines.getController(player).getInputCount(this) * 0.25);
            }
        }
    }
}
