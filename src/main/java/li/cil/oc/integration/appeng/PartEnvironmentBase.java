package li.cil.oc.integration.appeng;

import appeng.api.parts.IPartHost;
import appeng.util.ConfigInventory;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.ExtendedArgumentsJava;

interface PartEnvironmentBase extends ConfigEnvironment {
    IPartHost host();

    default Object[] getPartConfig(final Context context, final Arguments args) {
        final var side = new ExtendedArgumentsJava(args).checkSideAny(0);
        final var part = host().getPart(side);
        if (part instanceof appeng.helpers.IConfigInvHost) {
            return getConfig((ConfigInventory) ((appeng.helpers.IConfigInvHost) part).getConfig(), args, 1);
        }
        return new Object[]{null, "no matching part"};
    }

    default Object[] setPartConfig(final Context context, final Arguments args) {
        final var side = new ExtendedArgumentsJava(args).checkSideAny(0);
        final var part = host().getPart(side);
        if (part instanceof appeng.helpers.IConfigInvHost) {
            return setConfig((ConfigInventory) ((appeng.helpers.IConfigInvHost) part).getConfig(), context, args, 1, 2, 3, 4);
        }
        return new Object[]{null, "no matching part"};
    }
}
