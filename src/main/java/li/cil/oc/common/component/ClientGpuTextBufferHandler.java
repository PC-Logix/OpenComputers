package li.cil.oc.common.component;

import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.common.component.traits.VideoRamRasterizer;
import net.minecraft.core.component.DataComponentHolder;

import java.util.Optional;

public final class ClientGpuTextBufferHandler {
    private ClientGpuTextBufferHandler() { }

    public static void bitblt(TextBuffer dst, int col, int row, int width, int height,
                              String owner, int srcId, int fromCol, int fromRow) {
        if (!(dst instanceof VideoRamRasterizer device)) return;
        Optional<GpuTextBuffer> source = device.getBufferOptional(owner, srcId);
        source.ifPresent(value -> GpuTextBuffer.bitblt(dst, col, row, width, height, value, fromCol, fromRow));
    }

    public static boolean removeBuffer(TextBuffer buffer, String owner, int id) {
        return buffer instanceof VideoRamRasterizer device && device.removeBuffer(owner, id);
    }

    public static boolean loadBuffer(TextBuffer buffer, String owner, int id, DataComponentHolder data) {
        return buffer instanceof VideoRamRasterizer device && device.loadBuffer(owner, id, data);
    }
}
