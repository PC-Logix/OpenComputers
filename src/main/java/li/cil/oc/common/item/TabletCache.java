package li.cil.oc.common.item;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableMap;
import li.cil.oc.api.network.Node;
import li.cil.oc.util.ItemUtils;
import li.cil.oc.SettingsAccessJava;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.Optional;

/** Owns the bounded live tablet caches and their eviction lifecycle. */
public final class TabletCache {
    private static final Client CLIENT = new Client();
    private static final Server SERVER = new Server();

    private TabletCache() { }

    public static Client client() { return CLIENT; }
    public static Server server() { return SERVER; }

    public static Optional<TabletWrapper> getClientOptional(ItemStack stack) { return CLIENT.getOptional(stack); }
    public static Option<TabletWrapper> getClient(ItemStack stack) { return OptionConverters.toScala(getClientOptional(stack)); }
    public static Optional<TabletWrapper> getClientWeakOptional(ItemStack stack) { return CLIENT.getWeakOptional(stack); }
    public static Option<TabletWrapper> getClientWeak(ItemStack stack) { return OptionConverters.toScala(getClientWeakOptional(stack)); }
    public static TabletWrapper getClient(ItemStack stack, Player holder) {
        return CLIENT.get(stack, holder);
    }

    public static void invalidate(ItemStack stack) { SERVER.invalidate(stack); }

    public static Optional<String> getIdOptional(ItemStack stack) {
        CompoundTag tag = ItemUtils.getTag(stack);
        String key = SettingsAccessJava.namespace() + "tablet";
        return tag != null && tag.contains(key, Tag.TAG_STRING)
            ? Optional.of(tag.getString(key))
            : Optional.empty();
    }

    public static Option<String> getId(ItemStack stack) { return OptionConverters.toScala(getIdOptional(stack)); }

    public static String getOrCreateId(ItemStack stack) {
        String[] id = new String[1];
        String key = SettingsAccessJava.namespace() + "tablet";
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            if (!data.contains(key)) data.putString(key, UUID.randomUUID().toString());
            id[0] = data.getString(key);
        });
        return id[0];
    }

    public static TabletWrapper get(ItemStack stack, Player holder) {
        return holder.level().isClientSide() ? CLIENT.get(stack, holder) : SERVER.get(stack, holder);
    }

    public abstract static class Cache implements Callable<TabletWrapper>, RemovalListener<String, TabletWrapper> {
        private final com.google.common.cache.Cache<String, TabletWrapper> cache;
        private ItemStack currentStack;
        private Player currentHolder;

        protected Cache(int timeoutSeconds) {
            cache = CacheBuilder.<String, TabletWrapper>newBuilder()
                .expireAfterAccess(timeoutSeconds, TimeUnit.SECONDS)
                .removalListener(this)
                .build();
        }

        public TabletWrapper get(ItemStack stack, Player holder) {
            String id = getOrCreateId(stack);
            synchronized (cache) {
                currentStack = stack;
                currentHolder = holder;

                if (holder.level().isClientSide()) {
                    Optional<TabletWrapper> weak = CLIENT.getWeakOptional(stack);
                    if (weak.isPresent()) {
                        TabletWrapper cached = weak.get();
                        int timesChanged = holder.getInventory().getTimesChanged();
                        if (timesChanged != cached.timesChanged) {
                            if (!cached.isDirty) {
                                cached.isDirty = true;
                                li.cil.oc.client.PacketSender.sendMachineItemStateRequest(stack, holder.level().registryAccess());
                            }
                            cached.timesChanged = timesChanged;
                        }
                    }
                }

                TabletWrapper wrapper = getOrCreate(id);
                if (holder.level() != wrapper.getEnvironmentLevel()) {
                    wrapper.writeToNBT(holder.registryAccess());
                    wrapper.autoSave = false;
                    cache.invalidate(id);
                    cache.cleanUp();
                    wrapper = getOrCreate(id);
                }

                currentStack = null;
                currentHolder = null;
                wrapper.stack = stack;
                wrapper.player = holder;
                return wrapper;
            }
        }

        private TabletWrapper getOrCreate(String id) {
            try {
                return cache.get(id, this);
            } catch (ExecutionException error) {
                throw new IllegalStateException("Failed to create a tablet wrapper", error.getCause());
            }
        }

        @Override
        public TabletWrapper call() throws Exception {
            return new TabletWrapper(currentStack, currentHolder);
        }

        @Override
        public void onRemoval(RemovalNotification<String, TabletWrapper> event) {
            TabletWrapper tablet = event.getValue();
            if (tablet == null || tablet.node() == null) return;

            tablet.machine().stop();
            if (tablet.autoSave) {
                tablet.data.isRunning = tablet.machine().isRunning();
                tablet.writeToNBT(tablet.player.registryAccess());
                tablet.setChanged();
            }

            for (Node node : tablet.machine().node().network().nodes()) node.remove();
        }

        public void clear(Level level) {
            synchronized (cache) {
                List<String> ids = new ArrayList<>();
                cache.asMap().forEach((id, tablet) -> {
                    if (tablet.getEnvironmentLevel() == level) {
                        if (tablet.node() != null) {
                            tablet.writeToNBT(tablet.player.registryAccess());
                            tablet.autoSave = false;
                        }
                        ids.add(id);
                    }
                });
                cache.invalidateAll(ids);
                cache.cleanUp();
            }
        }

        public void cleanUp() {
            synchronized (cache) {
                cache.cleanUp();
            }
        }

        public ImmutableMap<String, TabletWrapper> keepAlive() {
            return cache.getAllPresent(cache.asMap().keySet());
        }

        protected Optional<TabletWrapper> getCachedOptional(String id) {
            synchronized (cache) {
                return Optional.ofNullable(cache.getIfPresent(id));
            }
        }

        protected Option<TabletWrapper> getCached(String id) { return OptionConverters.toScala(getCachedOptional(id)); }

        protected Object cacheMonitor() { return cache; }

        protected void invalidateCached(String id) { cache.invalidate(id); }

        protected java.util.Collection<TabletWrapper> cachedValues() { return cache.asMap().values(); }

        protected TabletWrapper getCacheValueWithoutAccess(String id) { return cache.asMap().get(id); }
    }

    public static final class Client extends Cache {
        private Client() { super(5); }

        public Optional<TabletWrapper> getWeakOptional(ItemStack stack) {
            return getIdOptional(stack).map(this::cacheGetIfPresent);
        }

        public Option<TabletWrapper> getWeak(ItemStack stack) { return OptionConverters.toScala(getWeakOptional(stack)); }

        public Optional<TabletWrapper> getOptional(ItemStack stack) {
            return getIdOptional(stack).flatMap(this::getCachedOptional);
        }

        public Option<TabletWrapper> get(ItemStack stack) { return OptionConverters.toScala(getOptional(stack)); }

        private TabletWrapper cacheGetIfPresent(String id) {
            // The weak lookup intentionally does not refresh access time.
            return getCacheValueWithoutAccess(id);
        }
    }

    public static final class Server extends Cache {
        private Server() { super(10); }

        public Optional<TabletWrapper> getOptional(String id) { return getCachedOptional(id); }
        public Option<TabletWrapper> get(String id) { return OptionConverters.toScala(getOptional(id)); }

        public void invalidate(ItemStack stack) {
            String id = getOrCreateId(stack);
            synchronized (cacheMonitor()) {
                Optional<TabletWrapper> tablet = getCachedOptional(id);
                if (tablet.isPresent()) tablet.get().stack = stack;
                invalidateCached(id);
                cleanUp();
            }
        }

        public void save(Player player) {
            synchronized (cacheMonitor()) {
                for (TabletWrapper tablet : cachedValues()) {
                    if (tablet.player == player) tablet.writeToNBT(player.registryAccess());
                }
            }
        }

        public void saveAll(Level level) {
            synchronized (cacheMonitor()) {
                for (TabletWrapper tablet : cachedValues()) {
                    if (tablet.getEnvironmentLevel() == level) tablet.writeToNBT(tablet.player.registryAccess());
                }
            }
        }
    }
}
