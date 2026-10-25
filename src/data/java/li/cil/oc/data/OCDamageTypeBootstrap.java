package li.cil.oc.data;

import li.cil.oc.common.nanomachines.ControllerImplConstants;
import li.cil.oc.common.nanomachines.provider.HungryProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DeathMessageType;

class OCDamageTypeBootstrap {
    public static void bootstrap(BootstrapContext<DamageType> bootstrap) {
        bootstrap.register(HungryProvider.HUNGRY_DAMAGE_KEY, new DamageType(
            "oc.nanomachinesHungry", DamageScaling.NEVER, 0.1f, DamageEffects.HURT, DeathMessageType.DEFAULT
        ));

        bootstrap.register(ControllerImplConstants.OverloadDamageKey, new DamageType(
            "oc.nanomachinesOverload", DamageScaling.NEVER, 0.1f, DamageEffects.HURT, DeathMessageType.DEFAULT
        ));
    }
}
