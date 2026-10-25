package li.cil.oc.server.component;

import li.cil.oc.api.network.Node;
import li.cil.oc.common.blockentity.traits.RedstoneChangedEventArgs;

import java.util.ArrayList;
import java.util.List;

/** Java implementation of redstone change notifications sent to computers. */
public final class RedstoneSignallerJava {
    private RedstoneSignallerJava() { }

    public static void onRedstoneChanged(Node node, int wakeThreshold, boolean wakeNeighborsOnly,
                                         RedstoneChangedEventArgs args) {
        Object side = args.side() == null ? "wireless" : Integer.valueOf(args.side().ordinal());
        List<Object> flatArgs = new ArrayList<>();
        flatArgs.add("redstone_changed");
        flatArgs.add(side);
        flatArgs.add(Integer.valueOf(args.oldValue()));
        flatArgs.add(Integer.valueOf(args.newValue()));
        if (args.color() >= 0) flatArgs.add(Integer.valueOf(args.color()));
        node.sendToReachable("computer.signal", flatArgs.toArray());

        if (args.oldValue() < wakeThreshold && args.newValue() >= wakeThreshold) {
            if (wakeNeighborsOnly) node.sendToNeighbors("computer.start");
            else node.sendToReachable("computer.start");
        }
    }
}
