package li.cil.oc.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.util.ResultWrapper;
public interface LevelControl extends LevelAware, SideRestricted {
    @Callback(doc = "function(side:number):boolean, string -- Checks the contents of the block on the specified sides and returns the findings.")
    default Object[] detect(Context context, Arguments args) {
        BlockContent content = blockContent(checkSideForAction(args, 0));
        return ResultWrapper.result(content.detected(), content.kind());
    }
}
