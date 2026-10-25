package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.common.datacomponents.CompoundStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

/** Screen-buffer persistence work that does not depend on Scala trait dispatch. */
final class TextBufferPersistenceJava {
    private static final String CLIENT_BUFFER_COMPONENTS_TAG = li.cil.oc.Settings.namespace() + "clientBufferComponents";

    private CompoundTag pendingServerBufferData;
    private Level movingClientLevel;

    void registerMovingClientBuffer(TextBuffer buffer, Level level) {
        if (buffer instanceof li.cil.oc.common.component.TextBuffer moving && movingClientLevel != level) {
            // A captured block entity can be created before its snapshot supplies an address.
            // Do not retain a failed registration or later updates will target the wrong level.
            if (moving.registerClientBufferOnLevel(level)) movingClientLevel = level;
        }
    }

    void unregisterMovingClientBuffer(TextBuffer buffer) {
        if (buffer instanceof li.cil.oc.common.component.TextBuffer moving && movingClientLevel != null) {
            moving.unregisterClientBufferOnLevel(movingClientLevel);
            movingClientLevel = null;
        }
    }

    void reapplyTier(TextBuffer buffer, int tier) {
        // The buffer may have been forced before the screen tier was loaded.
        // Changing the maximum depth is safe after construction and keeps the
        // configured limits aligned with the host's actual tier.
        scala.Tuple2<Object, Object> resolution = li.cil.oc.Settings.get().screenResolutionsByTier()[tier];
        buffer.setMaximumResolution(((Number) resolution._1()).intValue(), ((Number) resolution._2()).intValue());
        buffer.setMaximumColorDepth(li.cil.oc.Settings.get().screenDepthsByTier()[tier]);
    }

    void deferServerBufferData(CompoundTag data) {
        pendingServerBufferData = data.copy();
    }

    boolean hasPendingServerBufferData() {
        return pendingServerBufferData != null;
    }

    void loadForServer(CompoundTag data, HolderLookup.Provider provider,
                       TextBuffer buffer, Runnable reapplyTier) throws UnrecoverablePersistanceException {
        loadServerBufferData(data, provider, buffer, reapplyTier);
    }

    void ensureServerBufferLoaded(boolean isServer, Level level, TextBuffer buffer,
                                  Runnable reapplyTier) throws UnrecoverablePersistanceException {
        if (isServer && level != null && hasPendingServerBufferData()) {
            CompoundTag data = pendingServerBufferData;
            loadServerBufferData(data, level.registryAccess(), buffer, reapplyTier);
            pendingServerBufferData = null;
        }
    }

    private static void loadServerBufferData(CompoundTag data, HolderLookup.Provider provider,
                                             TextBuffer buffer, Runnable reapplyTier)
        throws UnrecoverablePersistanceException {
        reapplyTier.run();
        buffer.loadData(data, provider);
        // Stored video limits can be stale when the screen tier changed.
        reapplyTier.run();
    }

    void saveForServer(CompoundTag data, HolderLookup.Provider provider, TextBuffer buffer) {
        // Retain existing auxiliary data if lifecycle ordering has not supplied a level yet.
        if (!hasPendingServerBufferData()) buffer.saveData(data, provider);
    }

    void loadForClient(CompoundTag data, HolderLookup.Provider provider, TextBuffer buffer,
                       Runnable reapplyTier) throws UnrecoverablePersistanceException {
        reapplyTier.run();
        if (data.contains(CLIENT_BUFFER_COMPONENTS_TAG)) {
            CompoundStorage storage = CompoundStorage.CODEC
                .parse(NbtOps.INSTANCE, data.get(CLIENT_BUFFER_COMPONENTS_TAG)).getOrThrow();
            buffer.loadData(storage);
        } else {
            buffer.loadData(data, provider);
        }
        // The saved maximum mode may have been written with the constructor-default tier.
        reapplyTier.run();
    }

    void saveForClient(CompoundTag data, TextBuffer buffer) {
        CompoundStorage storage = new CompoundStorage();
        buffer.saveData(storage);
        Tag encoded = CompoundStorage.CODEC.encodeStart(NbtOps.INSTANCE, storage).getOrThrow();
        data.put(CLIENT_BUFFER_COMPONENTS_TAG, encoded);
    }
}
