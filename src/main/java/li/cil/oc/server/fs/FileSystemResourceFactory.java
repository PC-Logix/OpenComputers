package li.cil.oc.server.fs;

import li.cil.oc.api.fs.FileSystem;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLLoader;

import java.io.File;

/** Resource-backed filesystem construction shared by mod and development layouts. */
public final class FileSystemResourceFactory {
    private FileSystemResourceFactory() { }

    public static FileSystem fromResource(ResourceLocation location, ClassLoader modLoader, ClassLoader contextLoader) {
        String innerPath = "assets/" + location.getNamespace() + "/" + (location.getPath().trim() + "/");

        File directory = FileSystemResourceJava.findDirectory(location, modLoader, contextLoader);
        if (directory != null) return new ReadOnlyFileSystem(directory);

        File file = FMLLoader.getLoadingModList().getModFileById(location.getNamespace())
                .getFile().getFilePath().toFile();
        if (!file.exists()) return null;
        if (!file.isDirectory()) return ZipFileInputStreamFileSystem.fromFile(file, innerPath);

        File resourceRoot = new File(file, innerPath);
        return resourceRoot.exists() && resourceRoot.isDirectory() ? new ReadOnlyFileSystem(resourceRoot) : null;
    }
}
