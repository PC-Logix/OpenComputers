package li.cil.oc.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.Packet;
import li.cil.oc.server.network.DebugNetwork.DebugNode;
import li.cil.oc.server.GeneralPacketSender;
import li.cil.oc.server.network.DebugNetwork;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import scala.Option;

import java.io.IOException;

/** Messaging callbacks exposed by the debug card. */
final class DebugCardCommunicationCallbacks {
    private DebugCardCommunicationCallbacks() { }

    static Object[] sendToClipboard(Option<DebugCardAccessContext> access, Arguments args) {
        DebugCardSupport.checkAccess(access);
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(args.checkString(0));
        if (player != null) {
            try {
                GeneralPacketSender.sendClipboard(player, args.checkString(1));
            } catch (IOException error) {
                throw sneaky(error);
            }
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(false, "no such player");
    }

    static Object[] sendToDebugCard(DebugCard card, Option<DebugCardAccessContext> access, Arguments args) {
        DebugCardSupport.checkAccess(access);
        String destination = args.checkString(0);
        Option<DebugNode> endpoint = DebugNetwork.getEndpoint(destination);
        if (endpoint.isDefined() && endpoint.get() != card) {
            Object[] data = new Object[Math.max(0, args.count() - 1)];
            for (int i = 1; i < args.count(); i++) data[i - 1] = args.checkAny(i);
            Packet packet = Network.newPacket(card.node().address(), destination, 0, data);
            endpoint.get().receivePacket(packet);
        }
        return ResultWrapper.result();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable error) throws T {
        throw (T) error;
    }
}
