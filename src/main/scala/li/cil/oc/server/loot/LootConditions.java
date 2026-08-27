package li.cil.oc.server.loot;

import li.cil.oc.OpenComputers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LootConditions {
    private static final DeferredRegister<LootItemConditionType> REGISTRY =
        DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, OpenComputers.ID());

    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> IS_CRAFTABLE =
        REGISTRY.register("is_craftable", () -> IsCraftable.TYPE);

    public static void init(IEventBus bus) {
        REGISTRY.register(bus);
    }

    private LootConditions() {
    }
}
