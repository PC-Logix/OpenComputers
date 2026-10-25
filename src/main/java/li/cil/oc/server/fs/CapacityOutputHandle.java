package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;

import java.io.IOException;

/** Output handle that updates its filesystem's capacity accounting. */
final class CapacityOutputHandle extends OutputHandle {
    private final Capacity owner;
    private final OutputHandle inner;

    CapacityOutputHandle(Capacity owner, OutputHandle inner) {
        super(owner, inner.handle(), inner.path(), inner.mode());
        this.owner = owner;
        this.inner = inner;
    }

    @Override public boolean isClosed() { return inner.isClosed(); }

    @Override
    public void restoreMode(Mode value) {
        super.restoreMode(value);
        inner.restoreMode(value);
    }

    @Override public long length() { return inner.length(); }
    @Override public long position() { return inner.position(); }
    @Override public void close() { inner.close(); }
    @Override public long seek(long to) throws IOException { return inner.seek(to); }
    @Override public int read(byte[] bytes) throws IOException { return inner.read(bytes); }

    @Override
    public void write(byte[] bytes) throws IOException {
        long previousLength = inner.length();
        long required = owner.accounting().requiredWrite(inner.position(), previousLength, bytes.length, mode().isAppend());
        owner.accounting().checkWrite(required);
        inner.write(bytes);
        owner.accounting().wrote(previousLength, inner.length());
    }
}
