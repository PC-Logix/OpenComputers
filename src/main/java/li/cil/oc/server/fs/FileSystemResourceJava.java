package li.cil.oc.server.fs;

import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

/** Resolves unpacked resources exposed by development and runtime class loaders. */
public final class FileSystemResourceJava {
    private FileSystemResourceJava() {
    }

    public static File findDirectory(ResourceLocation location, ClassLoader modLoader, ClassLoader contextLoader) {
        String innerPath = "assets/" + location.getNamespace() + "/" + location.getPath().trim() + "/";

        String modFolders = System.getProperty("fml.modFolders");
        if (modFolders != null) {
            for (String entry : modFolders.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                String[] parts = entry.split("%%", 2);
                if (parts.length == 2 && containsNamespace(parts[0], location.getNamespace())) {
                    File directory = new File(parts[1], innerPath);
                    if (directory.exists() && directory.isDirectory()) return directory;
                }
            }
        }

        Set<ClassLoader> loaders = new LinkedHashSet<>();
        if (modLoader != null) loaders.add(modLoader);
        if (contextLoader != null) loaders.add(contextLoader);
        for (ClassLoader loader : loaders) {
            URL url = loader.getResource(innerPath);
            if (url != null && "file".equals(url.getProtocol())) {
                try {
                    File directory = Paths.get(url.toURI()).toFile();
                    if (directory.exists() && directory.isDirectory()) return directory;
                } catch (Exception ignored) {
                    // Invalid or non-local classpath URLs are skipped, as in the Scala Try path.
                }
            }
        }

        return null;
    }

    private static boolean containsNamespace(String namespaces, String namespace) {
        for (String value : namespaces.split(",")) {
            if (value.equals(namespace)) return true;
        }
        return false;
    }
}
