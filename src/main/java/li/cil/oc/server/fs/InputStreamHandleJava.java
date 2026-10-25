package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Handle;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.ToLongFunction;

/** Handle state for filesystems backed by an input channel. */
public final class InputStreamHandleJava implements Handle {
    private final int handle;
    private final String path;
    private final Channel channel;
    private final ToLongFunction<String> length;
    private final Runnable removeFromOwner;

    public InputStreamHandleJava(int handle, String path, Channel channel,
                                 ToLongFunction<String> length, Runnable removeFromOwner) {
        this.handle = handle;
        this.path = path;
        this.channel = channel;
        this.length = length;
        this.removeFromOwner = removeFromOwner;
    }

    public int handle() { return handle; }
    public String path() { return path; }
    public Channel channel() { return channel; }
    public long position() { return channel.position(); }
    @Override public long length() { return length.applyAsLong(path); }

    @Override
    public void close() {
        if (channel.isOpen()) {
            removeFromOwner.run();
            channel.close();
        }
    }

    @Override public int read(byte[] into) throws IOException { return channel.read(into); }
    @Override public long seek(long to) throws IOException { return channel.position(to); }

    @Override
    public void write(byte[] value) throws IOException {
        throw new IOException("bad file descriptor");
    }

    public static int read(Channel channel, ByteBuffer destination) throws IOException {
        int count = destination.remaining();
        if (count == 0) return 0;
        byte[] buffer = new byte[count];
        int read = channel.read(buffer);
        if (read > 0) destination.put(buffer, 0, read);
        return read;
    }

    public interface Channel {
        boolean isOpen();
        void close();
        long position();
        long position(long newPosition);
        int read(byte[] destination);
    }
}
