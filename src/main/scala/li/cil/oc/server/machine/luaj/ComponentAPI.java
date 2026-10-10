package li.cil.oc.server.machine.luaj;

import li.cil.oc.api.network.Component;
import li.cil.oc.util.ScalaClosure$;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;
import li.cil.repack.org.luaj.vm2.lib.VarArgFunction;
import scala.collection.Iterator;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

public class ComponentAPI extends LuaJAPI {
    public ComponentAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        LuaValue component = LuaValue.tableOf();
        component.set("list", function(args -> {
            synchronized (components()) {
                String filter = args.isstring(1) ? args.tojstring(1) : null;
                boolean exact = args.optboolean(2, false);
                LuaValue table = LuaValue.tableOf(0, components().size());
                for (Map.Entry<String, String> entry : components().entrySet()) {
                    if (filter == null || (exact ? entry.getValue().equals(filter) : entry.getValue().contains(filter))) {
                        table.set(entry.getKey(), entry.getValue());
                    }
                }
                return table;
            }
        }));
        component.set("type", function(args -> {
            synchronized (components()) {
                String name = components().get(args.checkjstring(1));
                return name != null ? LuaValue.valueOf(name) : missingComponent();
            }
        }));
        component.set("slot", function(args -> {
            synchronized (components()) {
                String address = args.checkjstring(1);
                return components().containsKey(address)
                    ? LuaValue.valueOf(machine.host().componentSlot(address)) : missingComponent();
            }
        }));
        component.set("methods", function(args -> withComponent(args.checkjstring(1), target -> {
            LuaValue table = LuaValue.tableOf();
            machine.methods(target.host()).forEach((name, annotation) -> table.set(name, LuaValue.tableOf(new LuaValue[]{
                LuaValue.valueOf("direct"), LuaValue.valueOf(annotation.direct()),
                LuaValue.valueOf("getter"), LuaValue.valueOf(annotation.getter()),
                LuaValue.valueOf("setter"), LuaValue.valueOf(annotation.setter())
            })));
            return table;
        })));
        component.set("invoke", function(args -> {
            String address = args.checkjstring(1);
            String method = args.checkjstring(2);
            Object[] params = toArray(ScalaClosure$.MODULE$.toSimpleJavaObjects(args, 3).iterator());
            return owner().invoke(() -> {
                try {
                    return machine.invoke(address, method, params);
                } catch (Exception error) {
                    throw unchecked(error);
                }
            });
        }));
        component.set("doc", function(args -> withComponent(args.checkjstring(1), target -> {
            String method = args.checkjstring(2);
            var methods = machine.methods(target.host());
            return owner().documentation(() -> {
                var annotation = methods.get(method);
                return annotation == null ? null : annotation.doc();
            });
        })));
        lua().set("component", component);
    }

    private Varargs withComponent(String address, Function<Component, Varargs> action) {
        var found = node().network().node(address);
        if (found instanceof Component component && (component.canBeSeenFrom(node()) || component == node())) {
            return action.apply(component);
        }
        return missingComponent();
    }

    private static Varargs missingComponent() {
        return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"));
    }

    private static Object[] toArray(Iterator<Object> iterator) {
        ArrayList<Object> values = new ArrayList<>();
        while (iterator.hasNext()) values.add(iterator.next());
        return values.toArray();
    }

    private static VarArgFunction function(Function<Varargs, Varargs> handler) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return handler.apply(args);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException unchecked(Throwable error) throws T {
        throw (T) error;
    }
}
