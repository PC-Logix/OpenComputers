package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public final class PowerAcceptorHooks {
    private static volatile PowerAcceptorIntegration integration;

    private PowerAcceptorHooks() {}

    static PowerAcceptorIntegration integration() { return integration; }

    public static void install(PowerAcceptorIntegration value) {
        integration = value;
    }

    public static void onInitialize(PowerAcceptor host) {
        var current = integration;
        if (current != null) current.initialize(host);
    }

    public static void onUpdate(PowerAcceptor host) {
        var current = integration;
        if (current != null) current.update(host);
    }

    public static void onDispose(PowerAcceptor host) {
        var current = integration;
        if (current != null) current.dispose(host);
    }

    public static void onLoad(PowerAcceptor host, CompoundTag nbt, HolderLookup.Provider provider) {
        var current = integration;
        if (current != null) current.load(host, nbt, provider);
    }

    public static void onSave(PowerAcceptor host, CompoundTag nbt, HolderLookup.Provider provider) {
        var current = integration;
        if (current != null) current.save(host, nbt, provider);
    }
}
