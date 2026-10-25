package li.cil.oc.server.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/** Java-facing input channel contract shared by filesystem implementations. */
public interface InputChannel extends ReadableByteChannel, InputStreamHandleJava.Channel {
    @Override boolean isOpen();
    @Override void close();
    @Override long position();
    @Override long position(long newPosition);
    @Override int read(byte[] destination);

    @Override
    default int read(ByteBuffer destination) throws IOException {
        return InputStreamHandleJava.read(this, destination);
    }
}
