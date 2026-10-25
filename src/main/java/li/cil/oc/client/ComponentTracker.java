package li.cil.oc.client;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class ComponentTracker extends li.cil.oc.common.ComponentTracker {
    public static final ComponentTracker INSTANCE = new ComponentTracker();
    private ComponentTracker() {}

    @Override
    protected void clear(Level level) {
        if (level.isClientSide()) super.clear(level);
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        worldUnloaded(event);
    }
}
