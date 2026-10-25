package li.cil.oc.server.fs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/** Java implementation of the random access operations used by file-backed input channels. */
public final class FileInputChannelJava implements AutoCloseable {
    private final RandomAccessFile file;
    private final FileChannel channel;

    public FileInputChannelJava(File file) throws IOException {
        this.file = new RandomAccessFile(file, "r");
        channel = this.file.getChannel();
    }

    public long position(long newPosition) throws IOException {
        channel.position(newPosition);
        return channel.position();
    }

    public long position() throws IOException {
        return channel.position();
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    public boolean isOpen() {
        return channel.isOpen();
    }

    public int read(byte[] destination) throws IOException {
        return channel.read(ByteBuffer.wrap(destination));
    }
}
