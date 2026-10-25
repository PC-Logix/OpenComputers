package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Network lifecycle phases for native Java block-entity hosts, including audio devices. */
public final class NetworkBlockEntityLifecycleJava {
    private NetworkBlockEntityLifecycleJava() { }
    public static void initialize(NetworkBlockEntity host) {
        EnvironmentJava.initialize(host);
        PowerAcceptorLifecycleJava.initialize(host, true);
        ComponentInventoryLifecycleJava.initialize(host);
        TextBufferLifecycleJava.ensureLoaded(host);
    }
    public static void update(NetworkBlockEntity host) {
        EnvironmentJava.updateEntity(host);
        PowerAcceptorLifecycleJava.update(host, true);
        PowerBalancerJava.updateAfterEnvironment(host);
        HubLifecycleJava.updateAfterEnvironment(host);
        TextBufferLifecycleJava.updateAfterEnvironment(host);
        RedstoneLifecycleJava.update(host, true);
    }
    public static void dispose(NetworkBlockEntity host) {
        EnvironmentJava.dispose(host);
        ComponentInventoryLifecycleJava.dispose(host);
    }
    public static void loadServer(NetworkBlockEntity host, CompoundTag nbt, HolderLookup.Provider provider) {
        try { EnvironmentJava.loadNodeData(nbt, tag("node"), host.node(), host, provider); }
        catch (li.cil.oc.api.UnrecoverablePersistanceException error) { NetworkBlockEntityLifecycleJava.<RuntimeException>throwUnchecked(error); }
        PowerAcceptorLifecycleJava.load(host, true, nbt, provider);
        HubLifecycleJava.loadAfterEnvironment(host, nbt, provider);
        TextBufferLifecycleJava.loadServer(host, nbt, provider);
        RedstoneLifecycleJava.loadServer(host, RedstoneLifecycleJava.Phase.ENVIRONMENT, nbt);
    }
    public static void saveServer(NetworkBlockEntity host, CompoundTag nbt, HolderLookup.Provider provider, Runnable saveBase) {
        HubLifecycleJava.saveAfterEnvironment(host, nbt, provider, () -> {
            saveBase.run();
            EnvironmentJava.saveNodeData(nbt, tag("node"), host.node(), host, provider);
            PowerAcceptorLifecycleJava.save(host, true, nbt, provider);
        });
        TextBufferLifecycleJava.saveServer(host, nbt, provider);
        RedstoneLifecycleJava.saveServer(host, RedstoneLifecycleJava.Phase.ENVIRONMENT, nbt);
    }
    private static String tag(String name) { return li.cil.oc.SettingsAccessJava.namespace() + name; }
    @SuppressWarnings("unchecked") private static <E extends Throwable> void throwUnchecked(Throwable error) throws E { throw (E) error; }
}
