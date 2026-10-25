package li.cil.oc.server.fs;

import java.io.IOException;
import java.io.RandomAccessFile;

/** Java implementation of random access operations for file-backed output handles. */
public final class FileOutputHandleJava implements AutoCloseable {
    private final RandomAccessFile file;

    public FileOutputHandleJava(RandomAccessFile file, boolean truncate) throws IOException {
        this.file = file;
        if (truncate) file.setLength(0);
    }

    public long position() throws IOException { return file.getFilePointer(); }
    public long length() throws IOException { return file.length(); }
    @Override public void close() throws IOException { file.close(); }

    public long seek(long to) throws IOException {
        if (to < 0) throw new IOException("invalid offset");
        file.seek(to);
        return to;
    }

    public int read(byte[] value, boolean readable) throws IOException {
        if (readable) return file.read(value);
        throw new IOException("bad file descriptor");
    }

    public void write(byte[] value, boolean append) throws IOException {
        if (append) file.seek(file.length());
        file.write(value);
    }
}
