package li.cil.oc.server.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/** Java implementation of a ComputerCraft-backed writable filesystem handle. */
public final class ComputerCraftWritableHandle {
    private final SeekableByteChannel channel;

    public ComputerCraftWritableHandle(SeekableByteChannel channel) {
        this.channel = channel;
    }

    public long length() {
        try { return channel.size(); }
        catch (IOException exception) { throw sneaky(exception); }
    }

    public long position() {
        try { return channel.position(); }
        catch (IOException exception) { throw sneaky(exception); }
    }

    public void close() {
        try { channel.close(); }
        catch (IOException exception) { throw sneaky(exception); }
    }

    public long seek(long to) {
        if (to < 0) throw sneaky(new IOException("invalid offset"));
        try { channel.position(to); return to; }
        catch (IOException exception) { throw sneaky(exception); }
    }

    public int read(byte[] value, boolean readable) {
        if (readable) {
            try { return channel.read(ByteBuffer.wrap(value)); }
            catch (IOException exception) { throw sneaky(exception); }
        }
        throw sneaky(new IOException("bad file descriptor"));
    }

    public void write(byte[] value, boolean append) throws IOException {
        if (append) channel.position(channel.size());
        ByteBuffer buffer = ByteBuffer.wrap(value);
        while (buffer.hasRemaining()) channel.write(buffer);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable error) throws T { throw (T) error; }
}
