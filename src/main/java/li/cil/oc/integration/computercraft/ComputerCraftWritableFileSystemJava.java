package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.filesystem.WritableMount;
import li.cil.oc.api.fs.Mode;
import scala.Option;
import scala.Option$;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/** Java mount operations for the Scala path-dependent output-handle bridge. */
public final class ComputerCraftWritableFileSystemJava {
    private ComputerCraftWritableFileSystemJava() { }

    public static boolean delete(WritableMount mount, String path) {
        try { mount.delete(path); return true; }
        catch (Throwable ignored) { return false; }
    }

    public static boolean makeDirectory(WritableMount mount, String path) {
        try { mount.makeDirectory(path); return true; }
        catch (Throwable ignored) { return false; }
    }

    public static Option<SeekableByteChannel> open(WritableMount mount, String path, Mode mode) {
        try {
            Set<OpenOption> options = new HashSet<>();
            options.add(StandardOpenOption.WRITE);
            if (mode.isReadable()) options.add(StandardOpenOption.READ);
            if (!mode.requiresExisting()) options.add(StandardOpenOption.CREATE);
            if (mode.isTruncate()) options.add(StandardOpenOption.TRUNCATE_EXISTING);
            SeekableByteChannel channel = mount.openFile(path, options);
            if (mode.isAppend() && !mode.isReadable()) channel.position(channel.size());
            return Option$.MODULE$.apply(channel);
        } catch (Throwable ignored) {
            return Option$.MODULE$.empty();
        }
    }
}
