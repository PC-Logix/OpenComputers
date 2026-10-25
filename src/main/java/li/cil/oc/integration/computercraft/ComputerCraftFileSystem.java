package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.filesystem.Mount;
import li.cil.oc.server.fs.InputChannel;
import li.cil.oc.server.fs.InputStreamChannel;
import li.cil.oc.server.fs.JavaInputStreamFileSystem;
import scala.Option;
import scala.Option$;

import java.nio.channels.Channels;
import java.util.ArrayList;
import java.io.IOException;

public class ComputerCraftFileSystem<M extends Mount> extends JavaInputStreamFileSystem {
    protected final M mount;

    public ComputerCraftFileSystem(final M mount) {
        this.mount = mount;
    }

    public M mount() { return mount; }

    @Override public long spaceTotal() { return 0L; }
    @Override public long spaceUsed() { return 0L; }
    @Override public boolean exists(final String path) {
        try { return mount.exists(path); }
        catch (final IOException exception) { throw sneaky(exception); }
    }
    @Override public boolean isDirectory(final String path) {
        try { return mount.isDirectory(path); }
        catch (final IOException exception) { throw sneaky(exception); }
    }
    @Override public long lastModified(final String path) { return 0L; }
    @Override public long size(final String path) {
        try { return mount.getSize(path); }
        catch (final IOException exception) { throw sneaky(exception); }
    }

    @Override
    public String[] list(final String path) {
        final var result = new ArrayList<String>();
        try {
            mount.list(path, result);
            return result.toArray(String[]::new);
        } catch (final IOException exception) {
            throw sneaky(exception);
        }
    }

    @Override
    public Option<InputChannel> openInputChannel(final String path) {
        try {
            final var input = Channels.newInputStream(mount.openForRead(path));
            return Option$.MODULE$.apply(new InputStreamChannel(input));
        } catch (final Throwable ignored) {
            return Option$.MODULE$.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(final Throwable throwable) throws T {
        throw (T) throwable;
    }
}
