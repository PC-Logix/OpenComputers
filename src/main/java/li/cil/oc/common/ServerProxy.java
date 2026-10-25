package li.cil.oc.common;

import li.cil.oc.server.driver.Registry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ServerProxy extends Proxy {
    public ServerProxy(IEventBus modBus) {
        super(modBus);
    }

    @SubscribeEvent
    public void postInit(FMLLoadCompleteEvent event) {
        Registry.locked = true;
    }

    @SubscribeEvent
    public void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        registerPacket(event);
    }
}
