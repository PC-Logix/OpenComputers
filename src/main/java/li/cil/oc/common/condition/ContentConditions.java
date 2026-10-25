package li.cil.oc.common.condition;

import com.mojang.serialization.MapCodec;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ContentConditions {
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, OpenComputers.ID());

    static {
        CONDITIONS.register("tier4_enabled", () -> Tier4EnabledCondition.CODEC);
        CONDITIONS.register("openscreens_enabled", () -> OpenScreensEnabledCondition.CODEC);
    }

    private ContentConditions() {}

    public static ICondition tier4Enabled() { return Tier4EnabledCondition.INSTANCE; }
    public static ICondition openScreensEnabled() { return OpenScreensEnabledCondition.INSTANCE; }

    public static void init(IEventBus bus) { CONDITIONS.register(bus); }
}

final class Tier4EnabledCondition implements ICondition {
    static final Tier4EnabledCondition INSTANCE = new Tier4EnabledCondition();
    static final MapCodec<Tier4EnabledCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    private Tier4EnabledCondition() {}

    @Override
    public boolean test(IContext context) { return !Settings.get().hideTier4(); }

    @Override
    public MapCodec<? extends ICondition> codec() { return CODEC; }
}

final class OpenScreensEnabledCondition implements ICondition {
    static final OpenScreensEnabledCondition INSTANCE = new OpenScreensEnabledCondition();
    static final MapCodec<OpenScreensEnabledCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    private OpenScreensEnabledCondition() {}

    @Override
    public boolean test(IContext context) { return !Settings.get().hideOpenScreens(); }

    @Override
    public MapCodec<? extends ICondition> codec() { return CODEC; }
}
