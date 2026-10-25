package li.cil.oc.common;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.SafeThreadPool;
import li.cil.oc.util.ThreadPoolFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Future;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Auxiliary on-disk storage for large block entity and machine state. */
public final class SaveHandler {
    private static final String UUID_REGEX = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
    private static final long TIME_TO_HOLD_ONTO_OLD_SAVES = 60L * 1000L;

    public static final SaveHandler INSTANCE = new SaveHandler();
    public static boolean savingForClients;
    public static final SafeThreadPool stateSaveHandler = ThreadPoolFactory.createSafePool("SaveHandler", 1);
    public static final ConcurrentLinkedDeque<File> chunkDirs = new ConcurrentLinkedDeque<>();
    public static final ConcurrentHashMap<String, Future<?>> saving = new ConcurrentHashMap<>();

    private SaveHandler() {}

    public static void initialize() {}

    public static final class SaveDataEntry implements Runnable {
        private final File root;
        private final byte[] data;
        private final ChunkPos pos;
        private final String name;
        private final ResourceLocation dimension;

        public SaveDataEntry(File root, byte[] data, ChunkPos pos, String name, ResourceLocation dimension) {
            this.root = root;
            this.data = data;
            this.pos = pos;
            this.name = name;
            this.dimension = dimension;
        }

        @Override
        public void run() {
            File path = statePath(root);
            File dimPath = new File(path, dimension.toString().replace(':', '/').replace('.', '/'));
            File chunkPath = new File(dimPath, pos.x + "." + pos.z);
            chunkDirs.add(chunkPath);
            if (!chunkPath.exists()) chunkPath.mkdirs();
            File file = new File(chunkPath, name);
            File temporary = new File(chunkPath, name + ".tmp");
            try {
                FileOutputStream raw = new FileOutputStream(temporary);
                try (var output = new java.io.BufferedOutputStream(raw)) {
                    output.write(data);
                    output.flush();
                    raw.getFD().sync();
                }
                try {
                    Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException error) {
                OpenComputers.log().warn("Error saving auxiliary tile entity data to '{}'.", file.getAbsolutePath(), error);
            } finally {
                temporary.delete();
            }
        }
    }

    public static File savePath() {
        return ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT).resolve(Settings.savePath()).toFile();
    }

    public static File statePath() {
        return statePath(savePath());
    }

    public static void visitJava17(File statePath) throws IOException {
        Files.walkFileTree(statePath.toPath(), new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                file.toFile().setLastModified(System.currentTimeMillis());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException error) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException error) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static File statePath(File root) {
        return new File(root, "state");
    }

    public static void scheduleSave(MachineHost host, CompoundTag nbt, String name, byte[] data) {
        scheduleSave(BlockPositionsJava.apply(host), nbt, name, data);
    }

    public static void scheduleSave(MachineHost host, CompoundTag nbt, String name, scala.Function1<CompoundTag, ?> save) {
        scheduleSave(host, nbt, name, writeNBT(save));
    }

    public static void scheduleSave(EnvironmentHost host, CompoundTag nbt, String name, scala.Function1<CompoundTag, ?> save) {
        scheduleSave(BlockPositionsJava.apply(host), nbt, name, writeNBT(save));
    }

    public static void scheduleSave(Level world, double x, double z, CompoundTag nbt, String name, byte[] data) {
        scheduleSave(BlockPositionsJava.apply(x, 0, z, world), nbt, name, data);
    }

    public static void scheduleSave(Level world, double x, double z, CompoundTag nbt, String name, scala.Function1<CompoundTag, ?> save) {
        scheduleSave(world, x, z, nbt, name, writeNBT(save));
    }

    public static void scheduleSave(BlockPosition position, CompoundTag nbt, String name, byte[] data) {
        Level world = position.worldOptional().orElseThrow();
        if (world instanceof ServerLevel) {
            ResourceLocation dimension = world.dimension().location();
            ChunkPos chunk = new ChunkPos(position.x() >> 4, position.z() >> 4);
            nbt.putString("dimension", dimension.toString());
            nbt.putInt("chunkX", chunk.x);
            nbt.putInt("chunkZ", chunk.z);
            scheduleSave(dimension, chunk, name, data);
        }
    }

    private static byte[] writeNBT(scala.Function1<CompoundTag, ?> save) {
        CompoundTag temporary = new CompoundTag();
        save.apply(temporary);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            NbtIo.write(temporary, output);
            return bytes.toByteArray();
        } catch (IOException error) {
            return SaveHandler.<RuntimeException, byte[]>sneakyThrow(error);
        }
    }

    public static CompoundTag loadNBT(CompoundTag nbt, String name) {
        return parseNBT(load(nbt, name));
    }

    public static CompoundTag loadNBT(ResourceLocation dimension, ChunkPos chunk, String name) {
        waitForSaveToComplete(name);
        return parseNBT(load(dimension, chunk, name));
    }

    private static CompoundTag parseNBT(byte[] data) {
        if (data.length > 0) {
            try {
                return NbtIo.read(new DataInputStream(new ByteArrayInputStream(data)));
            } catch (Throwable error) {
                OpenComputers.log().warn("There was an error trying to restore a block's state from external data. This indicates that data was somehow corrupted.", error);
            }
        }
        return new CompoundTag();
    }

    public static byte[] load(CompoundTag nbt, String name) {
        String dimension = nbt.getString("dimension");
        ChunkPos chunk = new ChunkPos(nbt.getInt("chunkX"), nbt.getInt("chunkZ"));
        waitForSaveToComplete(name);
        return load(ResourceLocation.tryParse(dimension), chunk, name);
    }

    private static void waitForSaveToComplete(String name) {
        Future<?> future = saving.get(name);
        if (future != null) {
            try {
                future.get(120L, TimeUnit.SECONDS);
            } catch (TimeoutException error) {
                OpenComputers.log().warn("Waiting for state data to save took two minutes! Aborting.");
            } catch (CancellationException ignored) {
            } catch (Throwable error) {
                SaveHandler.<RuntimeException, Void>sneakyThrow(error);
            }
        }
        saving.remove(name);
    }

    public static void scheduleSave(ResourceLocation dimension, ChunkPos chunk, String name, scala.Function1<CompoundTag, ?> data) {
        scheduleSave(dimension, chunk, name, writeNBT(data));
    }

    public static void scheduleSave(ResourceLocation dimension, ChunkPos chunk, String name, byte[] data) {
        if (chunk == null) throw new IllegalArgumentException("chunk is null");
        File root = savePath();
        Optional<Future<?>> future = stateSaveHandler.withPoolJava(pool -> pool.submit(new SaveDataEntry(root, data, chunk, name, dimension)));
        future.ifPresent(value -> saving.put(name, value));
    }

    public static byte[] load(ResourceLocation dimension, ChunkPos chunk, String name) {
        if (chunk == null) throw new IllegalArgumentException("chunk is null");
        File path = statePath();
        File dimPath = new File(path, dimension.toString().replace(':', '/').replace('.', '/'));
        File chunkPath = new File(dimPath, chunk.x + "." + chunk.z);
        File file = new File(chunkPath, name);
        if (!file.exists()) return new byte[0];
        try (var input = new java.io.BufferedInputStream(new FileInputStream(file));
             var output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) if (read > 0) output.write(buffer, 0, read);
            return output.toByteArray();
        } catch (IOException error) {
            OpenComputers.log().warn("Error loading auxiliary tile entity data.", error);
            return new byte[0];
        }
    }

    public static void cleanSaveData() {
        File[] emptyDirs = savePath().listFiles((FileFilter) file -> file.isDirectory()
            && file.getName().matches(UUID_REGEX)
            && System.currentTimeMillis() - file.lastModified() > TIME_TO_HOLD_ONTO_OLD_SAVES
            && ((file.list() == null) || file.list().length == 0));
        if (emptyDirs != null) for (File dir : emptyDirs) if (dir != null) dir.delete();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWorldLoad(LevelEvent.Load event) {
        if (!event.getLevel().isClientSide()) {
            try {
                visitJava17(statePath());
            } catch (IOException error) {
                SaveHandler.<RuntimeException, Void>sneakyThrow(error);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldSave(LevelEvent.Save event) {
        stateSaveHandler.withPoolJava(pool -> pool.submit((Runnable) SaveHandler::cleanSaveData));
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T sneakyThrow(Throwable error) throws E {
        throw (E) error;
    }
}
