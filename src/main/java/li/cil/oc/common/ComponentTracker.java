package li.cil.oc.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.LevelEvent;
import scala.Option;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Keeps track of loaded components by ID across server and client levels. */
public abstract class ComponentTracker {
    private final Map<Level, Cache<String, ManagedEnvironment>> worlds = new IdentityHashMap<>();

    private Cache<String, ManagedEnvironment> components(Level level) {
        return worlds.computeIfAbsent(level, ignored ->
                CacheBuilder.<String, ManagedEnvironment>newBuilder().weakValues().build());
    }

    public synchronized void add(Level level, String address, ManagedEnvironment component) {
        components(level).put(address, component);
    }

    public synchronized void remove(Level level, ManagedEnvironment component) {
        Cache<String, ManagedEnvironment> cache = components(level);
        for (var entry : cache.asMap().entrySet()) {
            if (Objects.equals(entry.getValue(), component)) cache.invalidate(entry.getKey());
        }
        cache.cleanUp();
    }

    public synchronized Optional<ManagedEnvironment> getOptional(Level level, String address) {
        Cache<String, ManagedEnvironment> cache = components(level);
        cache.cleanUp();
        return Optional.ofNullable(cache.getIfPresent(address));
    }

    public synchronized Option<ManagedEnvironment> get(Level level, String address) {
        return Option.apply(getOptional(level, address).orElse(null));
    }

    public void worldUnloaded(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) clear(level);
    }

    protected synchronized void clear(Level level) {
        Cache<String, ManagedEnvironment> cache = worlds.remove(level);
        if (cache != null) {
            cache.invalidateAll();
            cache.cleanUp();
        }
    }
}
