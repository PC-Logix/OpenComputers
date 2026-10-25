package li.cil.oc.common.blockentity.traits;

import li.cil.oc.SettingsAccessJava;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Hub phases run after the environment and before inventory or computer callbacks. */
public final class HubLifecycleJava {
    private HubLifecycleJava() { }
    public static void updateAfterEnvironment(Object host) {
        if (host instanceof Hub hub) {
            tick(hub, hub.getLevel().getGameTime());
            PowerAcceptorLifecycleJava.updateAfterHub(host);
            PowerBalancerJava.updateAfterHub(host);
        }
    }
    static void tick(Hub hub, long gameTime) {
        hub.relayCooldown_$eq(HubPacketQueueJava.tick(hub.queue(), hub.relayCooldown(), hub.relayAmount(), hub.relayDelay(),
            gameTime, hub.packetsPerCycleAvg(), entry -> hub.relayPacketJava(entry.sourceSideOptional(), entry.packet())));
    }
    public static void loadAfterEnvironment(Object host, CompoundTag nbt, HolderLookup.Provider provider) {
        if (host instanceof Hub hub) {
            try {
                hub.relayCooldown_$eq(HubPersistenceJava.load(nbt, provider, tag("plugs"), tag("queue"), "side",
                    tag("relayCooldown"), hub.plugNodes(), hub.queue(), hub.relayCooldown()));
            } catch (li.cil.oc.api.UnrecoverablePersistanceException error) { HubLifecycleJava.<RuntimeException>throwUnchecked(error); }
            PowerAcceptorLifecycleJava.loadAfterHub(host, nbt, provider);
        }
    }
    public static void saveAfterEnvironment(Object host, CompoundTag nbt, HolderLookup.Provider provider, Runnable saveEnvironment) {
        if (host instanceof Hub hub) {
            synchronized (hub.queue()) {
                saveEnvironment.run();
                if (hub.isServer()) HubPersistenceJava.save(nbt, provider, tag("plugs"), tag("queue"), "side",
                    tag("relayCooldown"), hub.plugNodes(), hub.queue(), hub.relayCooldown());
            }
            PowerAcceptorLifecycleJava.saveAfterHub(host, nbt, provider);
        } else saveEnvironment.run();
    }
    private static String tag(String name) { return SettingsAccessJava.namespace() + name; }
    @SuppressWarnings("unchecked") private static <E extends Throwable> void throwUnchecked(Throwable error) throws E { throw (E) error; }
}
