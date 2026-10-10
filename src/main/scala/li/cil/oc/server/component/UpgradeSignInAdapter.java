package li.cil.oc.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.util.ExtendedArguments;

public class UpgradeSignInAdapter extends UpgradeSign {
    private final EnvironmentHost host;

    public UpgradeSignInAdapter(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
            .withComponent("sign", Visibility.Network).withConnector().create());
    }

    @Override
    public EnvironmentHost host() { return host; }

    @Callback(doc = "function(side:number):string -- Get the text on the sign on the specified side of the adapter.")
    public Object[] getValue(Context context, Arguments args) {
        return super.getValue(findSign(new ExtendedArguments.ExtendedArguments(args).checkSideAny(0)));
    }

    @Callback(doc = "function(side:number, value:string):string -- Set the text on the sign on the specified side of the adapter.")
    public Object[] setValue(Context context, Arguments args) {
        return super.setValue(findSign(new ExtendedArguments.ExtendedArguments(args).checkSideAny(0)), args.checkString(1));
    }
}
