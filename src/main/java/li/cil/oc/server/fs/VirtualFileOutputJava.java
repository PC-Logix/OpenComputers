package li.cil.oc.server.fs;

import java.io.IOException;
import java.util.List;

/** Read and write operations for virtual filesystem output handles. */
final class VirtualFileOutputJava {
    private VirtualFileOutputJava() { }

    static int read(List<Byte> data, long position, byte[] buffer) throws IOException {
        int available = Math.max(data.size() - (int) position, 0);
        if (available == 0) return -1;
        int count = Math.min(buffer.length, available);
        for (int index = 0; index < count; index++) {
            buffer[index] = data.get((int) position + index);
        }
        return count;
    }

    static long write(List<Byte> data, long position, byte[] buffer, boolean append) {
        if (append) position = data.size();
        int start = (int) position;
        int required = (start + buffer.length) - data.size();
        for (int index = 0; index < required; index++) data.add((byte) 0);
        for (int index = 0; index < buffer.length; index++) data.set(start + index, buffer[index]);
        return position + buffer.length;
    }
}
