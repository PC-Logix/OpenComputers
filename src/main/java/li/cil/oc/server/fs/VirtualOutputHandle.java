package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;

import java.io.IOException;

/** Writable handle over an in-memory virtual filesystem file. */
final class VirtualOutputHandle extends OutputHandle {
    private final VirtualFileSystem.VirtualFile file;
    private long position;

    VirtualOutputHandle(VirtualFileSystem.VirtualFile file, OutputStreamFileSystem owner,
                        int handle, String path, Mode initialMode) {
        super(owner, handle, path, initialMode);
        this.file = file;
        this.position = initialMode.isAppend() && !initialMode.isReadable() ? file.data().size() : 0;
    }

    @Override public long length() { return file.size(); }
    @Override public long position() { return position; }

    @Override
    public void close() {
        if (!isClosed()) {
            super.close();
            assert file.handle() == this;
            file.setHandle(null);
        }
    }

    @Override
    public long seek(long to) throws IOException {
        if (to < 0) throw new IOException("invalid offset");
        position = to;
        return position;
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        if (isClosed()) throw new IOException("file is closed");
        if (!mode().isReadable()) throw new IOException("bad file descriptor");
        int count = VirtualFileOutputJava.read(file.data(), position, bytes);
        if (count > 0) position += count;
        return count;
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        if (isClosed()) throw new IOException("file is closed");
        position = VirtualFileOutputJava.write(file.data(), position, bytes, mode().isAppend());
        file.setLastModified(System.currentTimeMillis());
    }
}
