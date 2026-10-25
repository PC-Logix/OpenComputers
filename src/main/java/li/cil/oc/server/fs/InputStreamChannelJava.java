package li.cil.oc.server.fs;

import java.io.IOException;
import java.io.InputStream;

/** Java implementation of position tracking for stream-backed input channels. */
public final class InputStreamChannelJava implements AutoCloseable {
    private final InputStream inputStream;
    private boolean open = true;
    private long position;

    public InputStreamChannelJava(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void close() throws IOException {
        if (open) {
            open = false;
            inputStream.close();
        }
    }

    public boolean isOpen() { return open; }
    public long position() { return position; }

    public long position(long newPosition) throws IOException {
        inputStream.reset();
        position = inputStream.skip(newPosition);
        return position;
    }

    public int read(byte[] destination) throws IOException {
        int read = inputStream.read(destination);
        position += read;
        return read;
    }
}
