package li.cil.oc.common.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.datacomponents.TerminalReference;
import li.cil.oc.util.ExtendedDataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import scala.collection.Seq;
import scala.collection.immutable.List;
import scala.collection.immutable.List$;

import java.util.NoSuchElementException;
import java.util.Objects;

public interface RemoteTerminalHost extends EnvironmentHost {
    Rack rack();
    int slot();
    li.cil.oc.api.network.Node node();
    TextBuffer buffer();
    double range();
    Seq<String> sidedKeys();

    default boolean hasAddress() {
        Rack rack = rack();
        if (rack != null && !rack.getEnvironmentLevel().isClientSide()) return node().address() != null;
        if (rack != null) {
            var data = rack.getMountableData(slot());
            return data != null && data.has(OCComponentsJava.ADDRESS.get());
        }
        return false;
    }

    default String address() {
        Rack rack = rack();
        if (!rack.getEnvironmentLevel().isClientSide()) return node().address();
        String address = rack.getMountableData(slot()).get(OCComponentsJava.ADDRESS.get());
        if (address == null) throw new NoSuchElementException("remote terminal address is not available");
        return address;
    }

    default boolean isRemoteUsable(Player player) {
        Rack rack = rack();
        if (player == null || !player.isAlive() || rack == null || !hasAddress()) return false;
        boolean inRange = player.distanceToSqr(rack.xPosition() + 0.5, rack.yPosition() + 0.5, rack.zPosition() + 0.5) < range() * range();
        if (!inRange) return false;
        return isAuthorizedRemote(player.getItemInHand(InteractionHand.MAIN_HAND));
    }

    default boolean isBufferUsable(TextBuffer candidate, Player player) {
        return candidate == buffer() && isRemoteUsable(player);
    }

    default boolean isAuthorizedRemote(ItemStack stack) {
        TerminalReference reference = stack.get(OCComponentsJava.TERMINAL_REFERENCE.get());
        return reference != null && Objects.equals(reference.server(), address()) && sidedKeys().contains(reference.key());
    }
}