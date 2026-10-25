package li.cil.oc.common.event;

import li.cil.oc.api.Network;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.network.Node;
import li.cil.oc.server.component.WirelessNetworkCard;
import net.neoforged.bus.api.SubscribeEvent;

public final class WirelessNetworkCardHandler {
    public static final WirelessNetworkCardHandler INSTANCE = new WirelessNetworkCardHandler();

    private WirelessNetworkCardHandler() {}

    @SubscribeEvent
    public void onMove(RobotMoveEvent.Post event) {
        Node machineNode = event.agent.machine().node();
        for (Node node : machineNode.reachableNodes()) {
            if (node.host() instanceof WirelessNetworkCard card) Network.updateWirelessNetwork(card);
        }
    }
}
