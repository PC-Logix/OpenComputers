package li.cil.oc.common.event;

import li.cil.oc.api.event.NetworkActivityEvent;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.common.blockentity.Case;
import li.cil.oc.server.component.Server;
import net.neoforged.bus.api.SubscribeEvent;

public final class NetworkActivityHandler {
    public static final NetworkActivityHandler INSTANCE = new NetworkActivityHandler();

    private NetworkActivityHandler() {}

    @SubscribeEvent
    public void onNetworkActivity(NetworkActivityEvent.Server event) {
        if (!(event.getBlockEntity() instanceof Rack rack)) return;
        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
            if (rack.getMountable(slot) instanceof Server server
                && server.componentSlot(event.getNode().address()) >= 0) {
                server.lastNetworkActivity_$eq(System.currentTimeMillis());
                rack.markChanged(slot);
            }
        }
    }

    @SubscribeEvent
    public void onNetworkActivity(NetworkActivityEvent.Client event) {
        if (event.getBlockEntity() instanceof Case computer) {
            computer.lastNetworkActivity_$eq(System.currentTimeMillis());
        }
    }
}
