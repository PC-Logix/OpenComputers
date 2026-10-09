import li.cil.oc.api.driver.MethodWhitelist;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.FilteredEnvironment;
import li.cil.oc.server.machine.ArgumentsImpl;
import li.cil.oc.server.machine.CallbackAnalyzer;
import org.apache.logging.log4j.LogManager;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class MachineCallbackMigrationTest {
    @Test
    public void argumentsRetainLuaConversions() {
        Arguments args = new ArgumentsImpl(
            "héllo".getBytes(StandardCharsets.UTF_8), Double.POSITIVE_INFINITY,
            Double.NaN, scala.runtime.BoxedUnit.UNIT, scala.None$.MODULE$, Map.of("name", "test"));

        assertEquals("héllo", args.checkString(0));
        assertEquals(Integer.MAX_VALUE, args.checkInteger(1));
        assertFalse(args.isInteger(2));
        assertNull(args.checkAny(3));
        assertNull(args.checkAny(4));
        assertEquals("test", args.checkTable(5).get("name"));
        assertEquals("héllo", args.toArray()[0]);
        assertEquals("fallback", args.optString(6, "fallback"));
    }

    @Test
    public void callbackPriorityAndGeneratedInvocationStayAvailable() throws Exception {
        CallbackAnalyzer analyzer = new CallbackAnalyzer(LogManager.getLogger());
        Map<String, CallbackAnalyzer.Callback> callbacks = analyzer.analyze(List.of(new LowPriority(), new HighPriority()));
        CallbackAnalyzer.ComponentCallback callback = (CallbackAnalyzer.ComponentCallback) callbacks.get("shared");

        assertEquals(HighPriority.class, callback.method.getDeclaringClass());
        assertArrayEquals(new Object[]{"high"}, callback.apply(new HighPriority(), null, new ArgumentsImpl()));
    }

    @Test
    public void whitelistAndDynamicFilterApplyBeforeDiscovery() {
        CallbackAnalyzer analyzer = new CallbackAnalyzer(LogManager.getLogger());
        Map<String, CallbackAnalyzer.Callback> callbacks = analyzer.analyze(List.of(new FilteredCallbacks()));

        assertTrue(callbacks.containsKey("enabled"));
        assertFalse(callbacks.containsKey("filtered"));
        assertFalse(callbacks.containsKey("unlisted"));
    }

    public static class LowPriority implements NamedBlock {
        @Override public String preferredName() { return "low"; }
        @Override public int priority() { return 0; }
        @Callback("shared") public Object[] value(Context context, Arguments args) { return new Object[]{"low"}; }
    }

    public static class HighPriority implements NamedBlock {
        @Override public String preferredName() { return "high"; }
        @Override public int priority() { return 10; }
        @Callback("shared") public Object[] value(Context context, Arguments args) { return new Object[]{"high"}; }
    }

    public static class FilteredCallbacks implements MethodWhitelist, FilteredEnvironment {
        @Override public String[] whitelistedMethods() { return new String[]{"enabled", "filtered"}; }
        @Override public boolean isCallbackEnabled(String name) { return !name.equals("filtered"); }
        @Callback public Object[] enabled(Context context, Arguments args) { return null; }
        @Callback public Object[] filtered(Context context, Arguments args) { return null; }
        @Callback public Object[] unlisted(Context context, Arguments args) { return null; }
    }
}
