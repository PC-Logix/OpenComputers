package li.cil.oc.common.entity;

import li.cil.oc.OpenComputers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, OpenComputers.ID());

    public static final DeferredHolder<EntityType<?>, EntityType<Drone>> DRONE =
            ENTITY_TYPES.register("drone", () -> EntityType.Builder.of(Drone::new, MobCategory.MISC)
                    .sized(12 / 16f, 6 / 16f).fireImmune().build("drone"));

    public static final DeferredHolder<EntityType<?>, EntityType<TrainRobot>> TRAIN_ROBOT =
            ENTITY_TYPES.register("train_robot", () -> EntityType.Builder.of(TrainRobot::new, MobCategory.MISC)
                    .sized(12 / 16f, 12 / 16f).fireImmune().build("train_robot"));

    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(TRAIN_ROBOT.get(), TrainRobot.createMobAttributes().build());
    }

    private EntityTypes() {
        throw new Error();
    }
}