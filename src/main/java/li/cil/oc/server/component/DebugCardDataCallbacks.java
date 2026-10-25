package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Tuple3;

/** Data-component persistence for the debug card's access and remote-link state. */
final class DebugCardDataCallbacks {
    private DebugCardDataCallbacks() { }

    static void load(DebugCard card, DataComponentHolder holder) {
        card.setAccessForJava(Option.apply(holder.get(OCComponentsJava.DEBUG_CARD_ACCESS_CONTEXT.get())));
        BlockPos position = holder.get(OCComponentsJava.DEBUG_CARD_REMOTE_NODE_POSITION.get());
        if (position != null) card.setRemoteNodePositionForJava(position.getX(), position.getY(), position.getZ());
    }

    static void save(DebugCard card, MutableDataComponentHolder holder) {
        Option<DebugCardAccessContext> access = card.accessForJava();
        if (access.isDefined()) access.get().saveData(holder);

        Option<?> savedPosition = card.remoteNodePositionForJava();
        if (savedPosition.isDefined()) {
            Tuple3<?, ?, ?> position = (Tuple3<?, ?, ?>) savedPosition.get();
            int x = ((Number) position._1()).intValue();
            int y = ((Number) position._2()).intValue();
            int z = ((Number) position._3()).intValue();
            holder.set(OCComponentsJava.DEBUG_CARD_REMOTE_NODE_POSITION.get(), new BlockPos(x, y, z));
        }
    }
}
