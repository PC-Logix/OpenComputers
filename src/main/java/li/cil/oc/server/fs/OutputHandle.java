package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import li.cil.oc.api.fs.Handle;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Objects;

/** File handle shared by output filesystems without a Scala path-dependent owner type. */
public abstract class OutputHandle implements Handle {
    private final WeakReference<OutputHandleOwner> owner;
    private final int handle;
    private final String path;
    private Mode mode;
    protected boolean closed;

    protected OutputHandle(OutputHandleOwner owner, int handle, String path, Mode mode) {
        this.owner = new WeakReference<>(Objects.requireNonNull(owner));
        this.handle = handle;
        this.path = Objects.requireNonNull(path);
        this.mode = Objects.requireNonNull(mode);
    }

    public OutputHandleOwner owner() { return owner.get(); }
    public final int handle() { return handle; }
    public final String path() { return path; }
    public final Mode mode() { return mode; }
    public boolean isClosed() { return closed; }

    public void restoreMode(Mode value) {
        mode = Objects.requireNonNull(value);
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            OutputHandleOwner currentOwner = owner.get();
            if (currentOwner != null) currentOwner.removeOutputHandle(handle);
        }
    }

    @Override public int read(byte[] into) throws IOException { throw new IOException("bad file descriptor"); }
    @Override public long seek(long to) throws IOException { throw new IOException("bad file descriptor"); }
}
