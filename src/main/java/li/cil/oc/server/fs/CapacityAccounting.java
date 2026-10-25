package li.cil.oc.server.fs;

import li.cil.oc.Settings;
import li.cil.oc.api.fs.Mode;

import java.io.IOException;

/** Mutable accounting for capacity-limited filesystems. */
public final class CapacityAccounting {
    private final Capacity owner;
    private final long fileCost;
    private long used;
    private boolean ignoreCapacity;

    CapacityAccounting(Capacity owner) {
        this(owner, Settings.get().fileCost());
    }

    private CapacityAccounting(Capacity owner, long fileCost) {
        this(owner, fileCost, CapacityJava.computeSize(owner, "/", fileCost));
    }

    CapacityAccounting(Capacity owner, long fileCost, long initialUsed) {
        this.owner = owner;
        this.fileCost = fileCost;
        this.used = initialUsed;
    }

    public long used() { return used; }
    public boolean ignoringCapacity() { return ignoreCapacity; }
    public void setIgnoringCapacity(boolean value) { ignoreCapacity = value; }
    public void adjust(long delta) { used = Math.max(0, used + delta); }
    public void refresh() { used = CapacityJava.computeSize(owner, "/", fileCost); }
    public long freedBy(String path) { return fileCost + owner.size(path); }
    public long directoryCost() { return fileCost; }

    public void checkDirectory() throws IOException {
        if (owner.capacity() - used < directoryCost() && !ignoreCapacity)
            throw new IOException("not enough space");
    }

    public long openingDelta(String path, Mode mode) throws IOException {
        long delta = owner.exists(path)
                ? mode.isTruncate() ? -(int) owner.size(path) : 0
                : fileCost;
        if (owner.capacity() - used < delta && !ignoreCapacity)
            throw new IOException("not enough space");
        return delta;
    }

    public void checkWrite(long required) throws IOException {
        if (owner.capacity() - used < required && !ignoreCapacity)
            throw new IOException("not enough space");
    }

    public long requiredWrite(long position, long previousLength, int bytes, boolean append) {
        return Math.max(0L, (append ? previousLength : position) + bytes - previousLength);
    }

    public void wrote(long previousLength, long newLength) {
        used += Math.max(0L, newLength - previousLength);
    }
}
