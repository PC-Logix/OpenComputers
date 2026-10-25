package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;

import java.io.IOException;
import java.io.RandomAccessFile;

/** Writable output handle backed by a random-access file. */
final class RandomAccessOutputHandle extends OutputHandle {
    private final FileOutputHandleJava delegate;

    RandomAccessOutputHandle(OutputStreamFileSystem owner, int handle, String path, Mode mode, RandomAccessFile file)
            throws IOException {
        super(owner, handle, path, mode);
        delegate = new FileOutputHandleJava(file, mode.isTruncate());
    }

    @Override public long position() { return call(delegate::position); }
    @Override public long length() { return call(delegate::length); }

    @Override
    public void close() {
        super.close();
        run(delegate::close);
    }

    @Override public long seek(long to) throws IOException { return delegate.seek(to); }
    @Override public int read(byte[] value) throws IOException { return delegate.read(value, mode().isReadable()); }
    @Override public void write(byte[] value) throws IOException { delegate.write(value, mode().isAppend()); }

    private static void run(IoAction action) {
        try { action.run(); }
        catch (IOException error) { throw sneaky(error); }
    }

    private static <T> T call(IoSupplier<T> action) {
        try { return action.get(); }
        catch (IOException error) { throw sneaky(error); }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable error) throws T {
        throw (T) error;
    }

    @FunctionalInterface private interface IoAction { void run() throws IOException; }
    @FunctionalInterface private interface IoSupplier<T> { T get() throws IOException; }
}
