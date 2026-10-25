package li.cil.oc.common.event;

import li.cil.oc.common.EventHandler;
import li.cil.oc.util.BlockPosition;
import scala.runtime.BoxedUnit;

import java.util.Map;
import java.util.WeakHashMap;

@Deprecated
public final class BlockChangeHandler {
    private static final Map<ChangeListener, BlockPosition> CHANGE_LISTENERS = new WeakHashMap<>();

    private BlockChangeHandler() {}

    public static void addListener(ChangeListener listener, BlockPosition coord) {
        EventHandler.scheduleServer(() -> {
            CHANGE_LISTENERS.put(listener, coord);
            return BoxedUnit.UNIT;
        });
    }

    public static void removeListener(ChangeListener listener) {
        EventHandler.scheduleServer(() -> {
            CHANGE_LISTENERS.remove(listener);
            return BoxedUnit.UNIT;
        });
    }

    public interface ChangeListener {
        void onBlockChanged();
    }
}
