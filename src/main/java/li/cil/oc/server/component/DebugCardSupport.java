package li.cil.oc.server.component;

import li.cil.oc.Settings;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.world.level.Level;
import scala.Option;

/** Java helpers shared by the debug card and its userdata callbacks. */
public final class DebugCardSupport {
    private DebugCardSupport() { }

    public static void checkAccess(Option<DebugCardAccessContext> context) {
        Option<String> error = ((Settings.DebugCardAccess) Settings.get().debugCardAccess()).checkAccess(context);
        if (error.isDefined()) throw sneaky(new Exception(error.get()));
    }

    public static Object[] test(EnvironmentHost host, Option<DebugCardAccessContext> access) {
        checkAccess(access);
        scala.collection.mutable.HashMap<Object, Object> mutableValue = new scala.collection.mutable.HashMap<>();
        mutableValue.update("a", Boolean.TRUE);
        mutableValue.update("b", "test");

        Object mapOps = scala.collection.immutable.Map$.MODULE$.<Object, Object>empty()
            .updated(10, "zxc")
            .updated(Boolean.FALSE, mutableValue);
        @SuppressWarnings("unchecked")
        scala.collection.immutable.Map<Object, Object> immutableValue =
            (scala.collection.immutable.Map<Object, Object>) mapOps;
        mutableValue.update("c", immutableValue);

        return ResultWrapper.result(immutableValue, new DebugCard$TestValue(), host.getEnvironmentLevel());
    }

    public static AbstractValue worldValue(Level world, Option<DebugCardAccessContext> context) {
        return new DebugCard$WorldValue(world, context);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable error) throws T {
        throw (T) error;
    }
}
