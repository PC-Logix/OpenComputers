package li.cil.oc.common.blockentity.traits;

import li.cil.oc.SettingsAccessJava;
import net.minecraft.core.Direction;

/** Queue tuning and the six plug environments belong to the concrete Java host. */
public final class HubStateJava {
    private final HubRuntimeStateJava runtime;
    private final Hub.Plug[] plugs;
    public HubStateJava(Hub host) {
        this(host, SettingsAccessJava.get().switchDefaultMaxQueueSize(),
            SettingsAccessJava.get().switchDefaultRelayDelay(), SettingsAccessJava.get().switchDefaultRelayAmount());
    }
    public HubStateJava(Hub host, int maxQueueSize, int relayDelay, int relayAmount) {
        runtime = new HubRuntimeStateJava(maxQueueSize, relayDelay, relayAmount);
        plugs = new Hub.Plug[Direction.values().length];
        for (Direction side : Direction.values()) plugs[side.ordinal()] = host.createPlug(side);
    }
    public HubRuntimeStateJava runtime() { return runtime; }
    public Hub.Plug[] plugs() { return plugs; }
}
