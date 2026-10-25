package li.cil.oc.common.nanomachines;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public final class ControllerImplConstants {
    public static final ResourceKey<DamageType> OverloadDamageKey = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("opencomputers", "nanomachines_overload"));

    private ControllerImplConstants() { }
}
