package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Screen callbacks preserve deferred external data loading and the tier-before-mode restore order. */
public final class TextBufferLifecycleJava {
    private TextBufferLifecycleJava() { }
    public static void ensureLoaded(Object host) { if (host instanceof TextBuffer screen) screen.ensureServerBufferLoaded(); }
    public static void updateAfterEnvironment(Object host) {
        if (host instanceof TextBuffer screen && (screen.isClient() || EnvironmentJava.isConnected(screen.node()))) screen.buffer().update();
    }
    public static void reapplyTier(Object host) {
        if (host instanceof TextBuffer screen) screen.textBufferState().persistence.reapplyTier(screen.buffer(), screen.tier());
    }
    public static void loadServer(Object host, CompoundTag nbt, HolderLookup.Provider provider) {
        if (host instanceof TextBuffer screen) {
            var persistence = screen.textBufferState().persistence;
            if (screen.getLevel() == null) persistence.deferServerBufferData(nbt);
            else try { persistence.loadForServer(nbt, provider, screen.buffer(), () -> reapplyTier(screen)); }
            catch (li.cil.oc.api.UnrecoverablePersistanceException error) { throwUnchecked(error); }
        }
    }
    public static void saveServer(Object host, CompoundTag nbt, HolderLookup.Provider provider) {
        if (host instanceof TextBuffer screen) screen.textBufferState().persistence.saveForServer(nbt, provider, screen.buffer());
    }
    public static void loadClient(Object host, CompoundTag nbt, HolderLookup.Provider provider) {
        if (host instanceof TextBuffer screen) {
            try { screen.textBufferState().persistence.loadForClient(nbt, provider, screen.buffer(), () -> reapplyTier(screen)); }
            catch (li.cil.oc.api.UnrecoverablePersistanceException error) { throwUnchecked(error); }
        }
    }
    public static void saveClient(Object host, CompoundTag nbt) {
        if (host instanceof TextBuffer screen) screen.textBufferState().persistence.saveForClient(nbt, screen.buffer());
    }
    @SuppressWarnings("unchecked") static <E extends Throwable> void throwUnchecked(Throwable error) throws E { throw (E) error; }
}
