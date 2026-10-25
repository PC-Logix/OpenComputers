package li.cil.oc.server.fs;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Input stream behavior for the in-memory virtual filesystem. */
final class VirtualFileInputStreamJava extends InputStream {
    private final List<Byte> data;
    private boolean closed;
    private int position;

    VirtualFileInputStreamJava(List<Byte> data) {
        this.data = data;
    }

    @Override
    public int available() {
        return closed ? 0 : Math.max(data.size() - position, 0);
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public int read() throws IOException {
        if (closed) throw new IOException("file is closed");
        if (available() == 0) return -1;
        return data.get(position++);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (closed) throw new IOException("file is closed");
        int count = available();
        if (count == 0) return -1;
        int bytesRead = Math.min(length, count);
        for (int index = 0; index < bytesRead; index++) buffer[offset + index] = data.get(position + index);
        position += bytesRead;
        return bytesRead;
    }

    @Override
    public void reset() throws IOException {
        if (closed) throw new IOException("file is closed");
        position = 0;
    }

    @Override
    public long skip(long count) throws IOException {
        if (closed) throw new IOException("file is closed");
        position = Math.min((int) (position + count), Integer.MAX_VALUE);
        return position;
    }
}
