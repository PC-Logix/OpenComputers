package li.cil.oc.server.fs;

import org.apache.logging.log4j.Logger;
import li.cil.oc.common.item.traits.FileSystemLike;
import li.cil.oc.integration.opencomputers.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import li.cil.oc.Settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Shared Java implementations of filesystem path and save-directory operations. */
public final class FileSystemPathJava {
    private FileSystemPathJava() {
    }

    public static boolean isCaseInsensitive(File saveDirectory, Logger logger) {
        try {
            final String uuid = java.util.UUID.randomUUID().toString();
            final File lowerCase = new File(saveDirectory, uuid + "oc_rox");
            final File upperCase = new File(saveDirectory, uuid + "OC_ROX");
            // Clean up any stale names before probing, matching the existing behavior.
            if (lowerCase.exists()) lowerCase.delete();
            if (upperCase.exists()) upperCase.delete();
            lowerCase.createNewFile();
            final boolean insensitive = upperCase.exists();
            lowerCase.delete();
            return insensitive;
        } catch (Throwable t) {
            logger.warn("Couldn't determine if file system is case sensitive, falling back to insensitive.", t);
            return true;
        }
    }

    public static boolean isValidFilename(String name) {
        for (int i = 0; i < name.length(); i++) {
            switch (name.charAt(i)) {
                case '\\':
                case ':':
                case '*':
                case '?':
                case '"':
                case '<':
                case '>':
                case '|':
                    return false;
                default:
                    break;
            }
        }
        return true;
    }

    public static String validatePath(String path) {
        if (!isValidFilename(path)) {
            return FileSystemPathJava.<RuntimeException, String>throwUnchecked(
                    new IOException("path contains invalid characters"));
        }
        return path;
    }

    public static boolean removeAddress(ItemStack stack) {
        if (stack.getItem() instanceof FileSystemLike) {
            CompoundTag data = Item.dataTag(stack);
            if (data.contains("node")) {
                CompoundTag nodeData = data.getCompound("node");
                if (nodeData.contains("address")) {
                    nodeData.remove("address");
                    return true;
                }
            }
        }
        return false;
    }

    public static File createSaveDirectory(String root) {
        File path = ServerLifecycleHooks.getCurrentServer()
                .getWorldPath(new LevelResource(Settings.savePath() + root)).toFile();
        if (!path.isDirectory()) path.delete();
        path.mkdirs();
        return path.exists() && path.isDirectory() ? path : null;
    }

    public static String[] segments(String path) {
        String[] parts = validatePath(path).split("/");
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!part.isEmpty()) result.add(part);
        }
        return result.toArray(String[]::new);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T throwUnchecked(Throwable error) throws E {
        throw (E) error;
    }
}
