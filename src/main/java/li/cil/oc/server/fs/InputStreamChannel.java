package li.cil.oc.server.fs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/** Input-channel adapter for streams, exposed as an ordinary Java type. */
public final class InputStreamChannel implements InputChannel {
    private final InputStreamChannelJava delegate;

    public InputStreamChannel(InputStream inputStream) {
        delegate = new InputStreamChannelJava(Objects.requireNonNull(inputStream));
    }

    @Override public boolean isOpen() { return delegate.isOpen(); }
    @Override public void close() { run(delegate::close); }
    @Override public long position() { return delegate.position(); }
    @Override public long position(long newPosition) { return call(() -> delegate.position(newPosition)); }
    @Override public int read(byte[] destination) { return call(() -> delegate.read(destination)); }
    @Override public int read(ByteBuffer destination) throws IOException { return InputChannel.super.read(destination); }

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
