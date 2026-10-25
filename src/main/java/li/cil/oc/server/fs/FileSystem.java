package li.cil.oc.server.fs;

import li.cil.oc.OpenComputers;
import li.cil.oc.SettingsAccessJava;
import li.cil.oc.api.detail.FileSystemAPI;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import scala.Option;

import java.io.File;

/** Java filesystem factories and API registration. */
public final class FileSystem {
    public static final FileSystemAPI INSTANCE = new Api();

    private FileSystem() {}

    public static boolean isCaseInsensitive() { return CaseSensitivity.VALUE; }

    private static final class CaseSensitivity {
        private static final boolean VALUE = SettingsAccessJava.get().forceCaseInsensitive() ||
            FileSystemPathJava.isCaseInsensitive(
                ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT).toFile(),
                OpenComputers.log());
    }

    public static boolean isValidFilename(String name) { return FileSystemPathJava.isValidFilename(name); }
    public static String validatePath(String path) { return FileSystemPathJava.validatePath(path); }

    public static li.cil.oc.api.fs.FileSystem fromResource(ResourceLocation location) {
        return FileSystemResourceFactory.fromResource(location, FileSystem.class.getClassLoader(),
            Thread.currentThread().getContextClassLoader());
    }
    public static li.cil.oc.api.fs.FileSystem fromResource(ResourceManager manager, ResourceLocation location) {
        return ResourceManagerFileSystem.fromResource(manager, location);
    }
    public static Option<byte[]> readResource(ResourceManager manager, ResourceLocation location) {
        return ResourceManagerFileSystem.readResource(manager, location);
    }
    public static Capacity fromSaveDirectory(String root, long capacity, boolean buffered) {
        File path = FileSystemPathJava.createSaveDirectory(root);
        if (path == null) return null;
        return buffered ? new BufferedFileSystem(path, capacity) : new ReadWriteFileSystem(path, capacity);
    }
    public static boolean removeAddress(ItemStack stack) { return FileSystemPathJava.removeAddress(stack); }
    public static li.cil.oc.api.fs.FileSystem fromMemory(long capacity) { return new RamFileSystem(capacity); }
    public static li.cil.oc.api.fs.FileSystem asReadOnly(li.cil.oc.api.fs.FileSystem filesystem) {
        return filesystem.isReadOnly() ? filesystem : new ReadOnlyWrapper(filesystem);
    }
    public static li.cil.oc.server.component.FileSystem asManagedEnvironment(
            li.cil.oc.api.fs.FileSystem filesystem, Label label, EnvironmentHost host, String sound, int speed) {
        return FileSystemEnvironmentFactoryJava.create(filesystem, label, host, sound, speed);
    }
    public static li.cil.oc.server.component.FileSystem asManagedEnvironment(
            li.cil.oc.api.fs.FileSystem filesystem, Label label, EnvironmentHost host, String sound, int speed,
            double readEnergyCost, double writeEnergyCost) {
        return FileSystemEnvironmentFactoryJava.create(filesystem, label, host, sound, speed, readEnergyCost, writeEnergyCost);
    }
    public static li.cil.oc.server.component.FileSystem asManagedEnvironment(
            li.cil.oc.api.fs.FileSystem filesystem, String label, EnvironmentHost host, String sound, int speed) {
        return asManagedEnvironment(filesystem, new ReadOnlyLabel(label), host, sound, speed);
    }
    public static li.cil.oc.server.component.FileSystem asManagedEnvironment(
            li.cil.oc.api.fs.FileSystem filesystem, Label label, EnvironmentHost host, String sound) {
        return asManagedEnvironment(filesystem, label, host, sound, 1);
    }
    public static li.cil.oc.server.component.FileSystem asManagedEnvironment(
            li.cil.oc.api.fs.FileSystem filesystem, String label, EnvironmentHost host, String sound) {
        return asManagedEnvironment(filesystem, new ReadOnlyLabel(label), host, sound, 1);
    }
    public static li.cil.oc.server.component.FileSystem asManagedEnvironment(li.cil.oc.api.fs.FileSystem filesystem, Label label) {
        return asManagedEnvironment(filesystem, label, null, null, 1);
    }
    public static li.cil.oc.server.component.FileSystem asManagedEnvironment(li.cil.oc.api.fs.FileSystem filesystem, String label) {
        return asManagedEnvironment(filesystem, new ReadOnlyLabel(label), null, null, 1);
    }
    public static li.cil.oc.server.component.FileSystem asManagedEnvironment(li.cil.oc.api.fs.FileSystem filesystem) {
        return asManagedEnvironment(filesystem, (Label) null, null, null, 1);
    }

    public abstract static class ItemLabel implements Label {
        private final ItemStack stack;
        protected ItemLabel(ItemStack stack) { this.stack = stack; }
        public ItemStack stack() { return stack; }
    }

    public static class ReadOnlyLabel extends ReadOnlyLabelJava {
        public ReadOnlyLabel(String label) { super(label); }
    }

    private static final class Api implements FileSystemAPI {
        @Override public li.cil.oc.api.fs.FileSystem fromResource(ResourceLocation location) {
            return FileSystem.fromResource(location);
        }
        @Override public li.cil.oc.api.fs.FileSystem fromSaveDirectory(String root, long capacity, boolean buffered) {
            return FileSystem.fromSaveDirectory(root, capacity, buffered);
        }
        @Override public li.cil.oc.api.fs.FileSystem fromMemory(long capacity) { return FileSystem.fromMemory(capacity); }
        @Override public li.cil.oc.api.fs.FileSystem asReadOnly(li.cil.oc.api.fs.FileSystem filesystem) {
            return FileSystem.asReadOnly(filesystem);
        }
        @Override public li.cil.oc.server.component.FileSystem asManagedEnvironment(
                li.cil.oc.api.fs.FileSystem filesystem, Label label, EnvironmentHost host, String sound, int speed) {
            return FileSystem.asManagedEnvironment(filesystem, label, host, sound, speed);
        }
        @Override public li.cil.oc.server.component.FileSystem asManagedEnvironment(
                li.cil.oc.api.fs.FileSystem filesystem, String label, EnvironmentHost host, String sound, int speed) {
            return FileSystem.asManagedEnvironment(filesystem, label, host, sound, speed);
        }
        @Override public li.cil.oc.server.component.FileSystem asManagedEnvironment(
                li.cil.oc.api.fs.FileSystem filesystem, Label label, EnvironmentHost host, String sound) {
            return FileSystem.asManagedEnvironment(filesystem, label, host, sound);
        }
        @Override public li.cil.oc.server.component.FileSystem asManagedEnvironment(
                li.cil.oc.api.fs.FileSystem filesystem, String label, EnvironmentHost host, String sound) {
            return FileSystem.asManagedEnvironment(filesystem, label, host, sound);
        }
        @Override public li.cil.oc.server.component.FileSystem asManagedEnvironment(li.cil.oc.api.fs.FileSystem filesystem, Label label) {
            return FileSystem.asManagedEnvironment(filesystem, label);
        }
        @Override public li.cil.oc.server.component.FileSystem asManagedEnvironment(li.cil.oc.api.fs.FileSystem filesystem, String label) {
            return FileSystem.asManagedEnvironment(filesystem, label);
        }
        @Override public li.cil.oc.server.component.FileSystem asManagedEnvironment(li.cil.oc.api.fs.FileSystem filesystem) {
            return FileSystem.asManagedEnvironment(filesystem);
        }
    }
}
