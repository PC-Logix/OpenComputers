package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.filesystem.WritableMount;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.server.fs.ComputerCraftWritableHandle;
import li.cil.oc.server.fs.InputChannel;
import li.cil.oc.server.fs.InputStreamChannel;
import li.cil.oc.server.fs.JavaOutputStreamFileSystem;
import li.cil.oc.server.fs.OutputHandle;
import scala.Option;
import scala.Option$;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;

/** Writable ComputerCraft filesystem adapter. */
public final class ComputerCraftWritableFileSystem extends JavaOutputStreamFileSystem {
    private final WritableMount mount;

    public ComputerCraftWritableFileSystem(WritableMount mount) {
        this.mount = mount;
    }

    public WritableMount mount() { return mount; }
    @Override public long spaceTotal() { return 0L; }
    @Override public long spaceUsed() { return 0L; }

    @Override public boolean exists(String path) {
        try { return mount.exists(path); }
        catch (IOException exception) { throw sneaky(exception); }
    }

    @Override public boolean isDirectory(String path) {
        try { return mount.isDirectory(path); }
        catch (IOException exception) { throw sneaky(exception); }
    }

    @Override public long lastModified(String path) { return 0L; }

    @Override public long size(String path) {
        try { return mount.getSize(path); }
        catch (IOException exception) { throw sneaky(exception); }
    }

    @Override public String[] list(String path) {
        ArrayList<String> result = new ArrayList<>();
        try {
            mount.list(path, result);
            return result.toArray(String[]::new);
        } catch (IOException exception) {
            throw sneaky(exception);
        }
    }

    @Override public Option<InputChannel> openInputChannel(String path) {
        try {
            return Option$.MODULE$.apply(new InputStreamChannel(Channels.newInputStream(mount.openForRead(path))));
        } catch (Throwable ignored) {
            return Option$.MODULE$.empty();
        }
    }

    @Override public boolean delete(String path) {
        return ComputerCraftWritableFileSystemJava.delete(mount, path);
    }

    @Override public boolean makeDirectory(String path) {
        return ComputerCraftWritableFileSystemJava.makeDirectory(mount, path);
    }

    @Override public Option<OutputHandle> openOutputHandle(int id, String path, Mode mode) {
        Option<SeekableByteChannel> opened = ComputerCraftWritableFileSystemJava.open(mount, path, mode);
        if (opened.isEmpty()) return Option$.MODULE$.empty();

        ComputerCraftWritableHandle delegate = new ComputerCraftWritableHandle(opened.get());
        return Option$.MODULE$.apply(new OutputHandle(this, id, path, mode) {
            @Override public long length() { return delegate.length(); }
            @Override public long position() { return delegate.position(); }
            @Override public void close() { super.close(); delegate.close(); }
            @Override public long seek(long to) { return delegate.seek(to); }
            @Override public int read(byte[] value) { return delegate.read(value, mode().isReadable()); }
            @Override public void write(byte[] value) throws IOException { delegate.write(value, mode().isAppend()); }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
