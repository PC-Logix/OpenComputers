package li.cil.oc.common.nanomachines;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.common.Tier;
import li.cil.oc.SettingsAccessJava;
import scala.Function0;
import scala.Option$;
import scala.collection.immutable.Seq;
import scala.collection.Iterator;
import scala.runtime.BoxedUnit;

/** Java implementation of queued nanomachine wireless responses. */
public final class ControllerResponse {
    private ControllerResponse() { }

    public static void respond(ControllerImpl controller, WirelessEndpoint endpoint, Seq<Object> data) {
        Function0<BoxedUnit> send = () -> {
            if (controller.responsePort() > 0) {
                double range = controller.CommandRange();
                double cost = SettingsAccessJava.get().wirelessCostPerRange()[Tier.Two] * range;
                if (controller.changeBuffer(-cost) > -0.1) {
                    Object[] payload = new Object[data.size() + 1];
                    payload[0] = "nanomachines";
                    Iterator<Object> values = data.iterator();
                    for (int i = 1; i < payload.length; i++) payload[i] = values.next();
                    var packet = Network.newPacket(controller.uuid(), null, controller.responsePort(), payload);
                    Network.sendWirelessPacket(controller, range, packet);
                }
            }
            return BoxedUnit.UNIT;
        };
        controller.queuedCommand_$eq(Option$.MODULE$.apply(send));
        controller.commandDelay_$eq((int) (SettingsAccessJava.get().nanomachinesCommandDelay() * 20));
    }
}
