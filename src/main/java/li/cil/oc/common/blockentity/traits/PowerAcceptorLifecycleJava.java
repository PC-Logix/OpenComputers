package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Explicit lifecycle phases replacing PowerAcceptor's Scala super-call chain. */
public final class PowerAcceptorLifecycleJava {
    private PowerAcceptorLifecycleJava() {}

    private static PowerAcceptor select(Object host, boolean afterEnvironment) {
        return host instanceof PowerAcceptor acceptor &&
            acceptor.powerLifecycleAfterEnvironment() == afterEnvironment ? acceptor : null;
    }

    public static void initialize(Object host, boolean afterEnvironment) {
        PowerAcceptor acceptor = select(host, afterEnvironment);
        if (acceptor != null) PowerAcceptorHooks.onInitialize(acceptor);
    }
    public static void update(Object host, boolean afterEnvironment) {
        PowerAcceptor acceptor = select(host, afterEnvironment);
        if (acceptor != null && !acceptor.powerLifecycleAfterHub()) PowerAcceptorHooks.onUpdate(acceptor);
    }
    public static void dispose(Object host) {
        if (host instanceof PowerAcceptor acceptor) PowerAcceptorHooks.onDispose(acceptor);
    }
    public static void load(Object host, boolean afterEnvironment, CompoundTag nbt, HolderLookup.Provider provider) {
        PowerAcceptor acceptor = select(host, afterEnvironment);
        if (acceptor != null && !acceptor.powerLifecycleAfterHub()) PowerAcceptorHooks.onLoad(acceptor, nbt, provider);
    }
    public static void save(Object host, boolean afterEnvironment, CompoundTag nbt, HolderLookup.Provider provider) {
        PowerAcceptor acceptor = select(host, afterEnvironment);
        if (acceptor != null && !acceptor.powerLifecycleAfterHub()) PowerAcceptorHooks.onSave(acceptor, nbt, provider);
    }
    public static void updateAfterHub(Object host) {
        if (host instanceof PowerAcceptor acceptor && acceptor.powerLifecycleAfterHub())
            PowerAcceptorHooks.onUpdate(acceptor);
    }
    public static void loadAfterHub(Object host, CompoundTag nbt, HolderLookup.Provider provider) {
        if (host instanceof PowerAcceptor acceptor && acceptor.powerLifecycleAfterHub())
            PowerAcceptorHooks.onLoad(acceptor, nbt, provider);
    }
    public static void saveAfterHub(Object host, CompoundTag nbt, HolderLookup.Provider provider) {
        if (host instanceof PowerAcceptor acceptor && acceptor.powerLifecycleAfterHub())
            PowerAcceptorHooks.onSave(acceptor, nbt, provider);
    }
}
