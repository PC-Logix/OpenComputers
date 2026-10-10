package li.cil.oc.server.machine.luac;

import li.cil.oc.api.network.Component;
import li.cil.oc.util.ExtendedLuaState$;
import li.cil.repack.com.naef.jnlua.JavaFunction;
import li.cil.repack.com.naef.jnlua.LuaState;
import scala.collection.Iterator;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.ToIntFunction;

public class ComponentAPI extends NativeLuaAPI {
    public ComponentAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        lua().newTable();
        add("list", state -> {
            synchronized (components()) {
                String filter = state.isString(1) ? state.toString(1) : null;
                boolean exact = state.isBoolean(2) ? state.toBoolean(2) : true;
                state.newTable(0, components().size());
                for (Map.Entry<String, String> entry : components().entrySet()) {
                    if (filter == null || (exact ? entry.getValue().equals(filter) : entry.getValue().contains(filter))) {
                        state.pushString(entry.getKey());
                        state.pushString(entry.getValue());
                        state.rawSet(-3);
                    }
                }
                return 1;
            }
        });
        add("type", state -> {
            synchronized (components()) {
                String name = components().get(state.checkString(1));
                if (name == null) return missingComponent(state);
                state.pushString(name);
                return 1;
            }
        });
        add("slot", state -> {
            synchronized (components()) {
                String address = state.checkString(1);
                if (!components().containsKey(address)) return missingComponent(state);
                state.pushInteger(machine.host().componentSlot(address));
                return 1;
            }
        });
        add("methods", state -> withComponent(state.checkString(1), target -> {
            state.newTable();
            machine.methods(target.host()).forEach((name, annotation) -> {
                state.pushString(name);
                state.newTable();
                state.pushBoolean(annotation.direct());
                state.setField(-2, "direct");
                state.pushBoolean(annotation.getter());
                state.setField(-2, "getter");
                state.pushBoolean(annotation.setter());
                state.setField(-2, "setter");
                state.rawSet(-3);
            });
            return 1;
        }));
        add("invoke", state -> {
            String address = state.checkString(1);
            String method = state.checkString(2);
            Object[] params = toArray(ExtendedLuaState$.MODULE$.extendLuaState(state).toSimpleJavaObjects(3).iterator());
            return owner().invoke(() -> {
                try {
                    return machine.invoke(address, method, params);
                } catch (Exception error) {
                    throw unchecked(error);
                }
            });
        });
        add("doc", state -> withComponent(state.checkString(1), target -> {
            String method = state.checkString(2);
            var methods = machine.methods(target.host());
            return owner().documentation(() -> {
                var annotation = methods.get(method);
                return annotation == null ? null : annotation.doc();
            });
        }));
        lua().setGlobal("component");
    }

    private int withComponent(String address, ToIntFunction<Component> action) {
        var found = node().network().node(address);
        if (found instanceof Component component && (component.canBeSeenFrom(node()) || component == node())) {
            return action.applyAsInt(component);
        }
        return missingComponent(lua());
    }

    private static int missingComponent(LuaState state) {
        state.pushNil();
        state.pushString("no such component");
        return 2;
    }

    private void add(String name, JavaFunction function) {
        lua().pushJavaFunction(function);
        lua().setField(-2, name);
    }

    private static Object[] toArray(Iterator<Object> iterator) {
        ArrayList<Object> values = new ArrayList<>();
        while (iterator.hasNext()) values.add(iterator.next());
        return values.toArray();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException unchecked(Throwable error) throws T {
        throw (T) error;
    }
}
