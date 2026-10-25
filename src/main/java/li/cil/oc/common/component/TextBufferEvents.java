package li.cil.oc.common.component;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Client world lifecycle hooks for text-buffer tracking. */
public final class TextBufferEvents {
    public static final TextBufferEvents INSTANCE = new TextBufferEvents();

    private TextBufferEvents() { }

    @SubscribeEvent
    public void onChunkUnloaded(ChunkEvent.Unload event) {
        TextBufferClientRegistry.onChunkUnloaded(event);
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        TextBufferClientRegistry.onWorldUnload(event);
    }
}
