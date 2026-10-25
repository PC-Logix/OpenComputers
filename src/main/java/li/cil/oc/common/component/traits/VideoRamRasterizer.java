package li.cil.oc.common.component.traits;

import li.cil.oc.common.component.GpuTextBuffer;
import li.cil.oc.util.TextBuffer;
import net.minecraft.core.component.DataComponentHolder;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface VideoRamRasterizer {
    Map<String, VideoRamDevice> videoRamDevices();

    void onBufferRamInit(GpuTextBuffer ram);

    void onBufferBitBlt(int col, int row, int width, int height, GpuTextBuffer ram, int fromCol, int fromRow);

    void onBufferRamDestroy(GpuTextBuffer ram);

    class VirtualRamDevice implements VideoRamDevice {
        private final String owner;
        private final Map<Integer, GpuTextBuffer> buffers = new HashMap<>();

        public VirtualRamDevice(String owner) {
            this.owner = owner;
        }

        public String owner() {
            return owner;
        }

        @Override
        public Map<Integer, GpuTextBuffer> videoRamBuffers() {
            return buffers;
        }
    }

    default boolean addBuffer(GpuTextBuffer ram) {
        VideoRamDevice gpu = videoRamDevices().get(ram.owner());
        if (gpu == null) {
            gpu = new VirtualRamDevice(ram.owner());
            videoRamDevices().put(ram.owner(), gpu);
        }
        boolean preexists = gpu.addBuffer(ram);
        if (!preexists || ram.dirty()) {
            onBufferRamInit(ram);
        }
        return preexists;
    }

    default boolean removeBuffer(String owner, int id) {
        VideoRamDevice gpu = videoRamDevices().get(owner);
        if (gpu == null) return false;
        Optional<GpuTextBuffer> ram = gpu.getBufferOptional(id);
        if (ram.isEmpty()) return false;
        onBufferRamDestroy(ram.get());
        return gpu.removeBuffers(new int[]{id}) == 1;
    }

    default int removeAllBuffers(String owner) {
        int count = 0;
        VideoRamDevice gpu = videoRamDevices().get(owner);
        if (gpu != null) {
            for (int id : gpu.bufferIndexes()) {
                if (removeBuffer(owner, id)) count++;
            }
        }
        return count;
    }

    default int removeAllBuffers() {
        int count = 0;
        for (String owner : videoRamDevices().keySet()) {
            count += removeAllBuffers(owner);
        }
        return count;
    }

    default boolean loadBuffer(String owner, int id, DataComponentHolder holder) {
        TextBuffer source = VideoRamBufferSupport.newBuffer();
        source.loadData(holder);
        return addBuffer(GpuTextBuffer.wrap(owner, id, source));
    }

    default Optional<GpuTextBuffer> getBufferOptional(String owner, int id) {
        VideoRamDevice gpu = videoRamDevices().get(owner);
        return gpu == null ? Optional.empty() : gpu.getBufferOptional(id);
    }

    default Option<GpuTextBuffer> getBuffer(String owner, int id) {
        return OptionConverters.toScala(getBufferOptional(owner, id));
    }
}
