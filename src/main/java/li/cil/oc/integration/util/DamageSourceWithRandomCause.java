package li.cil.oc.integration.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class DamageSourceWithRandomCause extends DamageSource {
    private final ResourceKey<DamageType> key;
    private final int numCauses;

    public DamageSourceWithRandomCause(ResourceKey<DamageType> key, int numCauses, Level level) {
        super(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
        this.key = key;
        this.numCauses = numCauses;
    }

    public ResourceKey<DamageType> key() {
        return key;
    }

    public int numCauses() {
        return numCauses;
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity damagee) {
        LivingEntity damager = damagee.getKillCredit();
        String format = "death.attack." + type().msgId() + "." + (damagee.getRandom().nextInt(numCauses) + 1);
        return damager != null
            ? Component.translatable(format + ".player", damagee.getDisplayName(), damager.getDisplayName())
            : Component.translatable(format, damagee.getDisplayName());
    }
}
