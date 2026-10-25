package li.cil.oc.common;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.common.data.EEPROM;
import li.cil.oc.common.data.LootDisk;
import li.cil.oc.common.init.OCItems;
import li.cil.oc.server.fs.ResourceManagerFileSystem;
import li.cil.oc.util.Color;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import scala.Option;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.HashMap;
import scala.collection.mutable.HashSet;
import scala.collection.mutable.Map;
import scala.jdk.javaapi.CollectionConverters;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Random;
import java.util.Optional;
import java.util.concurrent.Callable;

/** Loot-backed disks and EEPROMs, including their datapack reload lifecycle. */
public final class Loot {
    public static final Loot INSTANCE = new Loot();

    public static final Map<ResourceLocation, Callable<FileSystem>> factories = new HashMap<>();
    public static final ArrayBuffer<Tuple2<ItemStack, Object>> worldDisks = new ArrayBuffer<>();
    public static final ArrayBuffer<ItemStack> disksForCyclingServer = new ArrayBuffer<>();
    public static final ArrayBuffer<ItemStack> disksForCyclingClient = new ArrayBuffer<>();
    public static final ArrayBuffer<ItemStack> disksForSampling = new ArrayBuffer<>();
    public static final ArrayBuffer<ItemStack> disksForClient = new ArrayBuffer<>();
    public static final ArrayBuffer<ItemStack> eepromsForServer = new ArrayBuffer<>();
    public static final ArrayBuffer<ItemStack> eepromsForClient = new ArrayBuffer<>();
    public static final scala.collection.mutable.Set<String> lootDiskDescriptorIds = new HashSet<>();

    private static final ArrayBuffer<Tuple2<ItemStack, Object>> datapackDisks = new ArrayBuffer<>();
    private static final ArrayBuffer<ItemStack> datapackCyclingDisks = new ArrayBuffer<>();
    private static final ArrayBuffer<Tuple2<ResourceLocation, ItemStack>> datapackEEPROMs = new ArrayBuffer<>();
    private static final Map<ResourceLocation, Callable<FileSystem>> datapackFactories = new HashMap<>();
    private static final Map<ResourceLocation, Option<Callable<FileSystem>>> datapackPreviousFactories = new HashMap<>();

    private static final ResourceLocation defaultEEPROMId = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), Constants.ItemName$.MODULE$.LuaBios());
    private static final ResourceLocation defaultOpenOSId = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), Constants.ItemName$.MODULE$.OpenOS());

    private Loot() {}

    public static ArrayBuffer<ItemStack> disksForCycling() {
        return !disksForCyclingClient.isEmpty() ? disksForCyclingClient : disksForCyclingServer;
    }

    public static synchronized ItemStack defaultEEPROM() {
        for (Tuple2<ResourceLocation, ItemStack> entry : CollectionConverters.asJava(datapackEEPROMs)) {
            if (entry._1().equals(defaultEEPROMId)) return entry._2().copy();
        }
        for (ItemStack stack : CollectionConverters.asJava(eepromsForClient)) {
            if ("EEPROM (Lua BIOS)".equals(stack.get(OCComponentsJava.LABEL.get()))) return stack.copy();
        }
        return ItemStack.EMPTY;
    }

    public static synchronized ItemStack defaultOpenOS() {
        for (Tuple2<ItemStack, Object> entry : CollectionConverters.asJava(datapackDisks)) {
            if (defaultOpenOSId.equals(entry._1().get(OCComponentsJava.LOOT_DISK.get()))) return entry._1().copy();
        }
        var item = OCItems.get(Constants.ItemName$.MODULE$.OpenOS());
        return item == null ? ItemStack.EMPTY : item.createItemStack(1);
    }

    public static synchronized void resetDisksForClient() {
        disksForClient.clear();
        for (Tuple2<ItemStack, Object> entry : CollectionConverters.asJava(datapackDisks)) {
            ItemStack stack = entry._1();
            if (!containsSameStack(disksForClient, stack)) disksForClient.append(stack.copy());
        }
    }

    public static boolean isLootDisk(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName$.MODULE$.Floppy())
            && stack.has(OCComponentsJava.LOOT_DISK.get());
    }

    public static Option<ItemStack> randomDisk(Random rng) {
        if (disksForSampling.isEmpty()) return Option.empty();
        return Option.apply(disksForSampling.apply(rng.nextInt(disksForSampling.length())));
    }

    /** Select a disk when the loot table is evaluated, after datapack reloads. */
    public static synchronized ItemStack randomDiskForLoot(RandomSource rng) {
        return disksForSampling.isEmpty() ? ItemStack.EMPTY : disksForSampling.apply(rng.nextInt(disksForSampling.length())).copy();
    }

    public static ItemStack registerLootDisk(String displayName, String name, ResourceLocation loc,
                                             DyeColor color, Callable<FileSystem> factory,
                                             boolean doRecipeCycling) {
        ItemStack stack = OCItems.get(Constants.ItemName$.MODULE$.Floppy()).createItemStack(1);
        stack.set(OCComponentsJava.LABEL, name);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(displayName));
        stack.set(OCComponentsJava.LOOT_DISK, loc);
        stack.set(OCComponentsJava.DISK_COLOR, color);

        OpenComputers.log().debug("Registering loot disk '{}' from mod {}: {}", name, loc.getNamespace(), stack);
        factories.update(loc, factory);
        if (doRecipeCycling && !containsLootDisk(disksForCyclingServer, loc)) disksForCyclingServer.append(stack);
        return stack.copy();
    }

    @SubscribeEvent
    public void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(new GsonBuilder().create(), LootDisk.DIRECTORY) {
            @Override
            protected void apply(java.util.Map<ResourceLocation, JsonElement> definitions, ResourceManager manager,
                                 ProfilerFiller profiler) {
                applyDatapackDisks(definitions, manager);
            }
        });
        event.addListener(new SimpleJsonResourceReloadListener(new GsonBuilder().create(), EEPROM.DIRECTORY) {
            @Override
            protected void apply(java.util.Map<ResourceLocation, JsonElement> definitions, ResourceManager manager,
                                 ProfilerFiller profiler) {
                applyDatapackEEPROMs(definitions, manager);
            }
        });
    }

    @SubscribeEvent
    public void initForWorld(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel world && world.dimension() == Level.OVERWORLD) {
            refreshWorldDisks(world.getServer());
        }
    }

    private static void refreshWorldDisks(net.minecraft.server.MinecraftServer server) {
        worldDisks.clear();
        disksForSampling.clear();

        File path = server.getWorldPath(new LevelResource(Settings.savePath())).toFile();
        if (path.exists() && path.isDirectory()) {
            File listFile = new File(path, "loot/loot.properties");
            if (listFile.exists() && listFile.isFile()) {
                try (FileInputStream stream = new FileInputStream(listFile)) {
                    Properties list = new Properties();
                    list.load(stream);
                    parseLootDisks(list, worldDisks, true);
                } catch (Throwable error) {
                    OpenComputers.log().warn("Failed opening loot descriptor file in saves folder.", error);
                }
            }
        }

        for (Tuple2<ItemStack, Object> entry : CollectionConverters.asJava(datapackDisks)) {
            if (!containsSameLootDisk(worldDisks, entry._1())) worldDisks.append(entry);
        }
        for (Tuple2<ItemStack, Object> entry : CollectionConverters.asJava(worldDisks)) {
            int count = ((Number) entry._2()).intValue();
            if (count > 0) for (int i = 0; i < count; i++) disksForSampling.append(entry._1());
        }
    }

    private static void refreshAndSyncWorldDisks() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        refreshWorldDisks(server);
        for (var player : server.getPlayerList().getPlayers()) li.cil.oc.server.LootPacketSender.sendDisksUnchecked(player);
    }

    private static boolean sameLootDisk(ItemStack left, ItemStack right) {
        return java.util.Objects.equals(left.get(OCComponentsJava.LOOT_DISK.get()), right.get(OCComponentsJava.LOOT_DISK.get()));
    }

    private static boolean containsSameLootDisk(ArrayBuffer<Tuple2<ItemStack, Object>> entries, ItemStack stack) {
        for (Tuple2<ItemStack, Object> entry : CollectionConverters.asJava(entries)) if (sameLootDisk(entry._1(), stack)) return true;
        return false;
    }

    private static boolean containsLootDisk(ArrayBuffer<ItemStack> entries, ResourceLocation id) {
        for (ItemStack entry : CollectionConverters.asJava(entries)) if (id.equals(entry.get(OCComponentsJava.LOOT_DISK.get()))) return true;
        return false;
    }

    private static boolean containsSameStack(ArrayBuffer<ItemStack> entries, ItemStack stack) {
        for (ItemStack entry : CollectionConverters.asJava(entries)) if (ItemStack.isSameItemSameComponents(entry, stack)) return true;
        return false;
    }

    private static void applyDatapackDisks(java.util.Map<ResourceLocation, JsonElement> definitions, ResourceManager manager) {
        synchronized (Loot.class) {
            clearDatapackDisks();
            definitions.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey(java.util.Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    ResourceLocation id = entry.getKey();
                    try {
                        LootDisk data = LootDisk.CODEC.parse(JsonOps.INSTANCE, entry.getValue()).getOrThrow();
                        ResourceLocation contents = id.withPrefix(LootDisk.DIRECTORY + "/");
                        FileSystem filesystem = ResourceManagerFileSystem.fromResource(manager, contents);
                        if (filesystem == null) throw new IllegalArgumentException("no filesystem resources found below " + contents);
                        Callable<FileSystem> factory = () -> filesystem;
                        datapackPreviousFactories.update(id, factories.get(id));
                        datapackFactories.update(id, factory);
                        boolean hadCyclingDisk = containsLootDisk(disksForCyclingServer, id);
                        ItemStack stack = registerLootDisk(data.label(), id.getPath(), id, data.color(), factory, data.recipeCycling());
                        datapackDisks.append(new Tuple2<>(stack, Integer.valueOf(data.weight())));
                        if (!containsSameStack(disksForClient, stack)) disksForClient.append(stack.copy());
                        if (data.recipeCycling() && !hadCyclingDisk) datapackCyclingDisks.append(stack);
                    } catch (Exception error) {
                        OpenComputers.log().warn("Skipping bad loot disk definition '{}'.", id, error);
                    }
                });
            refreshAndSyncWorldDisks();
        }
    }

    private static void clearDatapackDisks() {
        removeDatapackDiskCopies(disksForClient, datapackDisks);
        for (Tuple2<ItemStack, Object> entry : CollectionConverters.asJava(datapackDisks)) {
            removeMatchingWorldDisk(worldDisks, entry._1());
            removeMatchingSamplingDisk(disksForSampling, entry._1());
        }
        for (int i = disksForCyclingServer.length() - 1; i >= 0; i--) {
            if (containsSameStack(datapackCyclingDisks, disksForCyclingServer.apply(i))) disksForCyclingServer.remove(i);
        }
        Iterator<Tuple2<ResourceLocation, Callable<FileSystem>>> factoriesIterator = datapackFactories.iterator();
        while (factoriesIterator.hasNext()) {
            Tuple2<ResourceLocation, Callable<FileSystem>> entry = factoriesIterator.next();
            ResourceLocation id = entry._1();
            Option<Callable<FileSystem>> current = factories.get(id);
            if (current.isDefined() && current.get() == entry._2()) {
                Option<Option<Callable<FileSystem>>> previousValue = datapackPreviousFactories.get(id);
                Option<Callable<FileSystem>> previous = previousValue.isDefined() ? previousValue.get() : Option.empty();
                if (previous.isDefined()) factories.update(id, previous.get());
                else factories.remove(id);
            }
        }
        datapackDisks.clear();
        datapackCyclingDisks.clear();
        datapackFactories.clear();
        datapackPreviousFactories.clear();
    }

    private static void removeDatapackDiskCopies(ArrayBuffer<ItemStack> target, ArrayBuffer<Tuple2<ItemStack, Object>> datapack) {
        for (int i = target.length() - 1; i >= 0; i--) {
            if (containsSameStackInDatapack(target.apply(i), datapack)) target.remove(i);
        }
    }

    private static void removeDatapackEEPROMCopies(ArrayBuffer<ItemStack> target) {
        for (int i = target.length() - 1; i >= 0; i--) {
            for (Tuple2<ResourceLocation, ItemStack> entry : CollectionConverters.asJava(datapackEEPROMs)) {
                if (ItemStack.isSameItemSameComponents(target.apply(i), entry._2())) {
                    target.remove(i);
                    break;
                }
            }
        }
    }

    private static boolean containsSameStackInDatapack(ItemStack stack, ArrayBuffer<Tuple2<ItemStack, Object>> datapack) {
        for (Tuple2<ItemStack, Object> entry : CollectionConverters.asJava(datapack)) if (ItemStack.isSameItemSameComponents(stack, entry._1())) return true;
        return false;
    }

    private static void removeMatchingWorldDisk(ArrayBuffer<Tuple2<ItemStack, Object>> target, ItemStack stack) {
        for (int i = target.length() - 1; i >= 0; i--) if (sameLootDisk(target.apply(i)._1(), stack)) target.remove(i);
    }

    private static void removeMatchingSamplingDisk(ArrayBuffer<ItemStack> target, ItemStack stack) {
        for (int i = target.length() - 1; i >= 0; i--) if (sameLootDisk(target.apply(i), stack)) target.remove(i);
    }

    private static void applyDatapackEEPROMs(java.util.Map<ResourceLocation, JsonElement> definitions, ResourceManager manager) {
        synchronized (Loot.class) {
            removeDatapackEEPROMCopies(eepromsForServer);
            removeDatapackEEPROMCopies(eepromsForClient);
            datapackEEPROMs.clear();

            definitions.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey(java.util.Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    ResourceLocation id = entry.getKey();
                    try {
                        EEPROM eeprom = EEPROM.CODEC.parse(JsonOps.INSTANCE, entry.getValue()).getOrThrow();
                        byte[] code = eeprom.code().map(file -> readEEPROMResource("code", id, file, manager)).orElse(null);
                        byte[] data = eeprom.data().map(file -> readEEPROMResource("data", id, file, manager)).orElse(null);
                        ItemStack stack = OCItems.registerEEPROM(eeprom.label(), code, data, eeprom.readOnly());
                        datapackEEPROMs.append(new Tuple2<>(id, stack));
                        eepromsForServer.append(stack);
                        eepromsForClient.append(stack.copy());
                    } catch (Throwable error) {
                        OpenComputers.log().warn("Skipping bad EEPROM loot definition '{}'.", id, error);
                    }
                });

            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (var player : server.getPlayerList().getPlayers()) li.cil.oc.server.LootPacketSender.sendEepromsUnchecked(player);
            }
        }
    }

    private static byte[] readEEPROMResource(String field, ResourceLocation id, String file, ResourceManager manager) {
        ResourceLocation location = id.withPath(root -> EEPROM.DIRECTORY + "/" + root + "/" + file);
        Optional<byte[]> bytes = ResourceManagerFileSystem.readResourceOptional(manager, location);
        if (bytes.isPresent()) return bytes.get();
        throw new IllegalArgumentException("missing " + field + " resource " + location);
    }

    private static void parseLootDisks(Properties list, ArrayBuffer<Tuple2<ItemStack, Object>> acc, boolean external) {
        for (String key : list.stringPropertyNames()) {
            String value = list.getProperty(key);
            try {
                String[] parts = value.split(":");
                ItemStack stack;
                int count;
                if (parts.length == 3) {
                    stack = createLootDisk(parts[0], key, external, Option.apply(Color.byName(parts[2])));
                    count = Integer.parseInt(parts[1]);
                } else if (parts.length == 2) {
                    stack = createLootDisk(parts[0], key, external, Option.empty());
                    count = Integer.parseInt(parts[1]);
                } else {
                    stack = createLootDisk(value, key, external, Option.empty());
                    count = 1;
                }
                acc.append(new Tuple2<>(stack, Integer.valueOf(count)));
            } catch (Throwable error) {
                OpenComputers.log().warn("Bad loot descriptor: {}", value, error);
            }
        }
    }

    public static ItemStack createLootDisk(String name, String path, boolean external, Option<DyeColor> color) {
        Callable<FileSystem> callable = external
            ? () -> li.cil.oc.api.FileSystem.asReadOnly(li.cil.oc.api.FileSystem.fromSaveDirectory("loot/" + path, 0, false))
            : () -> li.cil.oc.api.FileSystem.fromResource(ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), "loot/" + path));
        ItemStack stack = OCItems.registerFloppy(name, path,
            ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), path),
            color.isDefined() ? color.get() : DyeColor.LIGHT_GRAY, callable, true);
        lootDiskDescriptorIds.$plus$eq(path);
        return stack;
    }

    public static ItemStack createLootDisk(String name, String path, boolean external) {
        return createLootDisk(name, path, external, Option.empty());
    }
}
