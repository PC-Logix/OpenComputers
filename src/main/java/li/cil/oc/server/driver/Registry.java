package li.cil.oc.server.driver;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.detail.DriverAPI;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import scala.None$;
import scala.Product;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.HashSet;
import scala.math.ScalaNumber;
import scala.runtime.BoxedUnit;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The driver and converter registry used by the public driver API. */
public final class Registry implements DriverAPI {
    public static final Registry INSTANCE = new Registry();

    private static final List<DriverBlock> sidedBlocks = new ArrayList<>();
    private static final List<DriverItem> items = new ArrayList<>();
    private static final List<Converter> converters = new ArrayList<>();
    private static final List<EnvironmentProvider> environmentProviders = new ArrayList<>();
    private static final List<InventoryProvider> inventoryProviders = new ArrayList<>();

    // Kept as a Scala collection for the existing Scala item-driver mixin.
    public static final ArrayBuffer<Tuple2<ItemStack, scala.collection.mutable.Set<Class<?>>>> blacklist = new ArrayBuffer<>();
    public static boolean locked;

    private Registry() {
    }

    @Override
    public void add(DriverBlock driver) {
        checkUnlocked("drivers");
        if (!sidedBlocks.contains(driver)) {
            OpenComputers.log().debug("Registering block driver {}.", driver.getClass().getName());
            sidedBlocks.add(driver);
        }
    }

    @Override
    public void add(DriverItem driver) {
        checkUnlocked("drivers");
        if (!items.contains(driver)) {
            OpenComputers.log().debug("Registering item driver {}.", driver.getClass().getName());
            items.add(driver);
        }
    }

    @Override
    public void add(Converter converter) {
        checkUnlocked("converters");
        if (!converters.contains(converter)) {
            OpenComputers.log().debug("Registering converter {}.", converter.getClass().getName());
            converters.add(converter);
        }
    }

    @Override
    public void add(EnvironmentProvider provider) {
        checkUnlocked("environment providers");
        if (!environmentProviders.contains(provider)) {
            OpenComputers.log().debug("Registering environment provider {}.", provider.getClass().getName());
            environmentProviders.add(provider);
        }
    }

    @Override
    public void add(InventoryProvider provider) {
        checkUnlocked("inventory providers");
        if (!inventoryProviders.contains(provider)) {
            OpenComputers.log().debug("Registering inventory provider {}.", provider.getClass().getName());
            inventoryProviders.add(provider);
        }
    }

    private static void checkUnlocked(String kind) {
        if (locked) throw new IllegalStateException("Please register all " + kind + " in the init phase.");
    }

    @Override
    public DriverBlock driverFor(Level level, BlockPos pos, Direction side) {
        List<DriverBlock> matching = new ArrayList<>();
        for (DriverBlock driver : sidedBlocks) {
            if (driver.worksWith(level, pos, side)) matching.add(driver);
        }
        return matching.isEmpty() ? null : new CompoundBlockDriver(matching.toArray(new DriverBlock[0]));
    }

    @Override
    public DriverItem driverFor(ItemStack stack, Class<? extends EnvironmentHost> host) {
        if (stack.isEmpty()) return null;
        boolean hasHostAware = false;
        for (DriverItem driver : items) {
            if (driver instanceof HostAware aware && driver.worksWith(stack)) {
                hasHostAware = true;
                if (aware.worksWith(stack, host)) return driver;
            }
        }
        return hasHostAware ? null : driverFor(stack);
    }

    @Override
    public DriverItem driverFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (DriverItem driver : items) {
            if (driver.worksWith(stack)) return driver;
        }
        return null;
    }

    @Override
    @Deprecated
    public Class<?> environmentFor(ItemStack stack) {
        for (EnvironmentProvider provider : environmentProviders) {
            Class<?> environment = provider.getEnvironment(stack);
            if (environment != null) return environment;
        }
        return null;
    }

    @Override
    public Set<Class<?>> environmentsFor(ItemStack stack) {
        Set<Class<?>> environments = new LinkedHashSet<>();
        for (EnvironmentProvider provider : environmentProviders) {
            Class<?> environment = provider.getEnvironment(stack);
            if (environment != null) environments.add(environment);
        }
        return environments;
    }

    @Override
    public IItemHandler itemHandlerFor(ItemStack stack, Player player) {
        for (InventoryProvider provider : inventoryProviders) {
            if (provider.worksWith(stack, player)) {
                return InventoryUtils.asItemHandler(provider.getInventory(stack, player));
            }
        }
        return stack.getCapability(Capabilities.ItemHandler.ITEM);
    }

    @Override
    public List<DriverItem> itemDrivers() {
        return Collections.unmodifiableList(items);
    }

    public static void blacklistHost(ItemStack stack, Class<?> host) {
        Iterator<Tuple2<ItemStack, scala.collection.mutable.Set<Class<?>>>> entries = blacklist.iterator();
        while (entries.hasNext()) {
            Tuple2<ItemStack, scala.collection.mutable.Set<Class<?>>> entry = entries.next();
            if (ItemStack.isSameItem(entry._1(), stack)) {
                entry._2().addOne(host);
                return;
            }
        }
        HashSet<Class<?>> hosts = new HashSet<>();
        hosts.addOne(host);
        blacklist.addOne(new Tuple2<>(stack, hosts));
    }

    public static Object[] convert(Object[] values) {
        if (values == null) return null;
        Object[] converted = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            converted[i] = convertRecursively(values[i], new IdentityHashMap<>(), false);
        }
        return converted;
    }

    public static Object convertRecursively(Object value, IdentityHashMap<Object, Object> memo) {
        return convertRecursively(value, memo, false);
    }

    public static Object convertRecursively(Object value, IdentityHashMap<Object, Object> memo, boolean force) {
        if (value instanceof ScalaNumber number) value = number.underlying();
        if (!force && memo.containsKey(value)) return memo.get(value);

        if (value == null || value == BoxedUnit.UNIT || value == None$.MODULE$) return null;
        if (value instanceof Boolean || value instanceof Byte || value instanceof Character ||
            value instanceof Short || value instanceof Integer || value instanceof Long ||
            value instanceof Float || value instanceof Double || value instanceof String || value instanceof Value) {
            return value;
        }
        if (value instanceof Number number) return number.doubleValue();

        Class<?> type = value.getClass();
        if (type.isArray()) {
            Class<?> component = type.getComponentType();
            if (component.isPrimitive() || component == String.class || component == Boolean.class ||
                component == Byte.class || component == Character.class || component == Short.class ||
                component == Integer.class || component == Long.class || component == Float.class ||
                component == Double.class) return value;
            return convertArray(value, memo);
        }
        if (value instanceof Product product) return convertScalaIterator(value, product.productIterator(), memo);
        if (value instanceof scala.collection.Map<?, ?> map) return convertScalaMap(value, map.iterator(), memo);
        if (value instanceof scala.collection.Seq<?> sequence) return convertScalaIterator(value, sequence.iterator(), memo);
        if (value instanceof Map<?, ?> map) return convertJavaMap(value, map, memo);
        if (value instanceof scala.collection.Iterable<?> iterable) return convertScalaIterator(value, iterable.iterator(), memo);
        if (value instanceof Iterable<?> iterable) return convertJavaIterable(value, iterable, memo);

        Map<Object, Object> converted = new java.util.HashMap<>();
        memo.put(value, converted);
        for (Converter converter : converters) {
            try {
                converter.convert(value, converted);
            }
            catch (Throwable error) {
                OpenComputers.log().warn("Type converter threw an exception.", error);
            }
        }
        if (converted.isEmpty()) {
            String fallback = value.toString();
            memo.put(value, fallback);
            return fallback;
        }

        // Convert in place so recursive references to this map see its final entries.
        memo.put(converted, converted);
        convertJavaMap(converted, converted, memo);
        memo.remove(converted);
        if (converted.size() == 1 && converted.containsKey("oc:flatten")) {
            Object flattened = converted.get("oc:flatten");
            memo.put(value, flattened);
            return flattened;
        }
        return converted;
    }

    private static Object[] convertArray(Object array, IdentityHashMap<Object, Object> memo) {
        int length = Array.getLength(array);
        ArrayBuffer<Object> pending = new ArrayBuffer<>();
        memo.put(array, pending);
        Object[] converted = new Object[length];
        for (int i = 0; i < length; i++) {
            converted[i] = convertRecursively(Array.get(array, i), memo);
            pending.addOne(converted[i]);
        }
        return converted;
    }

    private static Object[] convertScalaIterator(Object source, Iterator<?> values, IdentityHashMap<Object, Object> memo) {
        ArrayBuffer<Object> pending = new ArrayBuffer<>();
        memo.put(source, pending);
        List<Object> converted = new ArrayList<>();
        while (values.hasNext()) {
            Object entry = convertRecursively(values.next(), memo);
            converted.add(entry);
            pending.addOne(entry);
        }
        return converted.toArray();
    }

    private static Object[] convertJavaIterable(Object source, Iterable<?> values, IdentityHashMap<Object, Object> memo) {
        ArrayBuffer<Object> pending = new ArrayBuffer<>();
        memo.put(source, pending);
        List<Object> converted = new ArrayList<>();
        for (Object value : values) {
            Object entry = convertRecursively(value, memo);
            converted.add(entry);
            pending.addOne(entry);
        }
        return converted.toArray();
    }

    private static Object convertScalaMap(Object source, Iterator<? extends Tuple2<?, ?>> entries,
                                          IdentityHashMap<Object, Object> memo) {
        Object target = memo.computeIfAbsent(source, ignored -> new scala.collection.mutable.HashMap<>());
        while (entries.hasNext()) {
            Tuple2<?, ?> entry = entries.next();
            if (entry._1() != null && entry._2() != null) {
                Object key = convertRecursively(entry._1(), memo);
                Object value = convertRecursively(entry._2(), memo);
                if (target instanceof scala.collection.mutable.Map<?, ?> scalaMap) {
                    @SuppressWarnings("unchecked") scala.collection.mutable.Map<Object, Object> writable =
                        (scala.collection.mutable.Map<Object, Object>) scalaMap;
                    writable.update(key, value);
                }
                else {
                    @SuppressWarnings("unchecked") Map<Object, Object> writable = (Map<Object, Object>) target;
                    writable.put(key, value);
                }
            }
        }
        return memo.get(source);
    }

    private static Object convertJavaMap(Object source, Map<?, ?> entries, IdentityHashMap<Object, Object> memo) {
        Object target = memo.computeIfAbsent(source, ignored -> new scala.collection.mutable.HashMap<>());
        for (Map.Entry<?, ?> entry : new ArrayList<>(entries.entrySet())) {
            if (entry.getKey() != null && entry.getValue() != null) {
                Object key = convertRecursively(entry.getKey(), memo);
                Object value = convertRecursively(entry.getValue(), memo);
                if (target instanceof scala.collection.mutable.Map<?, ?> scalaMap) {
                    @SuppressWarnings("unchecked") scala.collection.mutable.Map<Object, Object> writable =
                        (scala.collection.mutable.Map<Object, Object>) scalaMap;
                    writable.update(key, value);
                }
                else {
                    @SuppressWarnings("unchecked") Map<Object, Object> writable = (Map<Object, Object>) target;
                    writable.put(key, value);
                }
            }
        }
        return memo.get(source);
    }
}
