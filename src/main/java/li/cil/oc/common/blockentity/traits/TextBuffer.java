package li.cil.oc.common.blockentity.traits;

import net.minecraft.world.level.Level;

/** Managed screen-buffer capability with Java-owned lazy state. */
public interface TextBuffer extends li.cil.oc.api.network.Environment, li.cil.oc.api.network.EnvironmentHost, Tickable {
    TextBufferHostStateJava textBufferState();
    int tier();
    boolean isServer();
    boolean isClient();
    Level getLevel();
    default li.cil.oc.api.internal.TextBuffer buffer() { return textBufferState().buffer(this); }
    @Override default li.cil.oc.api.network.Node node() { return buffer().node(); }
    default void registerMovingClientBuffer(Level level) { textBufferState().persistence.registerMovingClientBuffer(buffer(), level); }
    default void unregisterMovingClientBuffer() { textBufferState().persistence.unregisterMovingClientBuffer(buffer()); }
    default void ensureServerBufferLoaded() {
        try {
            textBufferState().persistence.ensureServerBufferLoaded(isServer(), getLevel(), buffer(),
                () -> textBufferState().persistence.reapplyTier(buffer(), tier()));
        } catch (li.cil.oc.api.UnrecoverablePersistanceException error) { TextBufferLifecycleJava.throwUnchecked(error); }
    }
}
