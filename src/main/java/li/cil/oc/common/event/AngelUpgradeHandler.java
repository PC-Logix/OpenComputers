package li.cil.oc.common.event;

import li.cil.oc.api.event.RobotPlaceInAirEvent;
import li.cil.oc.api.network.Node;
import li.cil.oc.server.component.UpgradeAngel;
import net.neoforged.bus.api.SubscribeEvent;

public final class AngelUpgradeHandler {
    public static final AngelUpgradeHandler INSTANCE = new AngelUpgradeHandler();

    private AngelUpgradeHandler() {}

    @SubscribeEvent
    public void onPlaceInAir(RobotPlaceInAirEvent event) {
        Node machineNode = event.agent.machine().node();
        for (Node node : machineNode.reachableNodes()) {
            if (node.canBeReachedFrom(machineNode) && node.host() instanceof UpgradeAngel) {
                event.setAllowed(true);
                return;
            }
        }
        event.setAllowed(false);
    }
}
