package li.cil.oc.server.fs;

import java.io.File;
import java.io.IOException;

/** Input-channel adapter around a random-access file channel. */
final class FileInputChannel implements InputChannel {
    private final FileInputChannelJava delegate;

    FileInputChannel(File file) throws IOException {
        delegate = new FileInputChannelJava(file);
    }

    @Override public boolean isOpen() { return delegate.isOpen(); }
    @Override public void close() { run(delegate::close); }
    @Override public long position() { return call(delegate::position); }
    @Override public long position(long newPosition) { return call(() -> delegate.position(newPosition)); }
    @Override public int read(byte[] destination) { return call(() -> delegate.read(destination)); }

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
