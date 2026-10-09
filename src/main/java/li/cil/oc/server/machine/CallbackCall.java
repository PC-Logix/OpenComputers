package li.cil.oc.server.machine;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;

public interface CallbackCall {
    Object[] call(Object instance, Context context, Arguments args);
}
