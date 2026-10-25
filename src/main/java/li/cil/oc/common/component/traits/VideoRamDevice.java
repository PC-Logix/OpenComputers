package li.cil.oc.common.component.traits;

import li.cil.oc.common.component.GpuTextBuffer;
import li.cil.oc.util.TextBuffer;
import net.minecraft.core.component.DataComponentHolder;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;

import java.util.Map;
import java.util.Optional;

public interface VideoRamDevice {
    Map<Integer, GpuTextBuffer> videoRamBuffers();

    default int RESERVED_SCREEN_INDEX() {
        return 0;
    }

    default boolean isEmpty() {
        return videoRamBuffers().isEmpty();
    }

    default void onBufferRamDestroy(int id) {}

    default int[] bufferIndexes() {
        return videoRamBuffers().keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    default boolean addBuffer(GpuTextBuffer ram) {
        boolean preexists = videoRamBuffers().containsKey(ram.id());
        videoRamBuffers().put(ram.id(), ram);
        return preexists;
    }

    default int removeBuffers(int[] ids) {
        int count = 0;
        if (ids.length > 0) {
            for (int id : ids) {
                if (videoRamBuffers().remove(id) != null) {
                    onBufferRamDestroy(id);
                    count++;
                }
            }
        }
        return count;
    }

    default int removeAllBuffers() {
        return removeBuffers(bufferIndexes());
    }

    default void loadBuffer(String address, int id, DataComponentHolder holder) {
        TextBuffer source = VideoRamBufferSupport.newBuffer();
        source.loadData(holder);
        addBuffer(GpuTextBuffer.wrap(address, id, source));
    }

    default Optional<GpuTextBuffer> getBufferOptional(int id) {
        return Optional.ofNullable(videoRamBuffers().get(id));
    }

    default Option<GpuTextBuffer> getBuffer(int id) {
        return OptionConverters.toScala(getBufferOptional(id));
    }

    default int nextAvailableBufferIndex() {
        int index = RESERVED_SCREEN_INDEX() + 1;
        while (videoRamBuffers().containsKey(index)) {
            index++;
        }
        return index;
    }

    default int calculateUsedMemory() {
        int sum = 0;
        for (GpuTextBuffer buffer : videoRamBuffers().values()) {
            sum += buffer.data().width() * buffer.data().height();
        }
        return sum;
    }
}
