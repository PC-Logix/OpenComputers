package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractBehavior;
import li.cil.oc.api.prefab.AbstractProvider;
import li.cil.oc.integration.util.DamageSourceWithRandomCause;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class HungryProvider extends AbstractProvider {
    public static final HungryProvider INSTANCE = new HungryProvider();
    public static final int FILL_COUNT = 10;
    public static final ResourceKey<DamageType> HUNGRY_DAMAGE_KEY = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("opencomputers", "nanomachines_hungry"));

    private HungryProvider() { super("d697c24a-014c-4773-a288-23084a59e9e8"); }

    @Override
    public Iterable<Behavior> createBehaviors(Player player) {
        List<Behavior> behaviors = new ArrayList<>(FILL_COUNT);
        for (int i = 0; i < FILL_COUNT; i++) behaviors.add(new HungryBehavior(player));
        return behaviors;
    }

    @Override
    protected Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        return new HungryBehavior(player);
    }

    public static final class HungryBehavior extends AbstractBehavior {
        public HungryBehavior(Player player) { super(player); }

        @Override
        public void onDisable(DisableReason reason) {
            if (reason == DisableReason.OutOfEnergy) {
                player.hurt(new DamageSourceWithRandomCause(HUNGRY_DAMAGE_KEY, 3, player.level()),
                        Settings.get().nanomachinesHungryDamage());
                Nanomachines.getController(player).changeBuffer(Settings.get().nanomachinesHungryEnergyRestored());
            }
        }
    }
}
