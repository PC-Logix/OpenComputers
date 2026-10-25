package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractBehavior;
import li.cil.oc.api.prefab.AbstractProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PotionProvider extends AbstractProvider {
    public static final PotionProvider INSTANCE = new PotionProvider();

    private Set<MobEffect> potionWhitelist;

    private PotionProvider() { super("c29e4eec-5a46-479a-9b3d-ad0f06da784a"); }

    private synchronized Set<MobEffect> potionWhitelist() {
        if (potionWhitelist == null) potionWhitelist = filterPotions(Settings.get().nanomachinePotionWhitelist());
        return potionWhitelist;
    }

    public static Set<MobEffect> filterPotions(Iterable<?> names) {
        Set<MobEffect> result = new HashSet<>();
        for (Object name : names) {
            MobEffect effect = null;
            if (name instanceof String text) {
                effect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.tryParse(text));
            } else if (name instanceof ResourceLocation location) {
                effect = BuiltInRegistries.MOB_EFFECT.get(location);
            } else if (name instanceof Number id) {
                effect = BuiltInRegistries.MOB_EFFECT.getHolder(id.intValue()).map(Holder::value).orElseThrow();
            }
            if (effect != null) result.add(effect);
        }
        return result;
    }

    public boolean isPotionEligible(MobEffect potion) {
        return potion != null && potionWhitelist().contains(potion);
    }

    @Override
    public Iterable<Behavior> createBehaviors(Player player) {
        List<Behavior> behaviors = new ArrayList<>();
        BuiltInRegistries.MOB_EFFECT.asLookup().listElements()
                .filter(holder -> isPotionEligible(holder.value()))
                .forEach(holder -> behaviors.add(new PotionBehavior(holder, player)));
        return behaviors;
    }

    @Override
    protected void writeBehaviorToNBT(Behavior behavior, CompoundTag nbt) {
        if (behavior instanceof PotionBehavior potion) {
            var key = potion.effect.key();
            nbt.putString("potionId", key != null ? key.toString() : "minecraft:empty");
        }
    }

    @Override
    protected Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        var location = ResourceLocation.tryParse(nbt.getString("potionId"));
        return new PotionBehavior(BuiltInRegistries.MOB_EFFECT.getHolder(location).orElseThrow(), player);
    }

    public static final class PotionBehavior extends AbstractBehavior {
        public static final int DURATION = 600;
        public final Holder.Reference<MobEffect> effect;

        public PotionBehavior(Holder.Reference<MobEffect> effect, Player player) {
            super(player);
            this.effect = effect;
        }

        public int amplifier(Player player) {
            return Nanomachines.getController(player).getInputCount(this) - 1;
        }

        @Override
        public String getNameHint() {
            String name = effect.value().getDescriptionId();
            return name.startsWith("effect.") ? name.substring("effect.".length()) : name;
        }

        @Override
        public void onDisable(DisableReason reason) {
            player.removeEffect(effect);
        }

        @Override
        public void update() {
            player.addEffect(new MobEffectInstance(effect, DURATION, amplifier(player), true,
                    Settings.get().enableNanomachinePfx()));
        }
    }
}
