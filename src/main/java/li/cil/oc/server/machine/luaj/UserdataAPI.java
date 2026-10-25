package li.cil.oc.server.machine.luaj;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.machine.Value;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.server.machine.ArgumentsImpl;
import li.cil.oc.util.ScalaClosureSupport;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;
import li.cil.repack.org.luaj.vm2.lib.VarArgFunction;
import scala.collection.Iterator;

import java.util.ArrayList;
import java.util.function.Function;

public class UserdataAPI extends LuaJAPI {
    public UserdataAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        LuaValue userdata = LuaValue.tableOf();
        userdata.set("apply", function(args -> {
            Value value = (Value) args.checkuserdata(1, Value.class);
            Object[] params = toArray(ScalaClosureSupport.toSimpleJavaObjects(args, 2).iterator());
            return owner().invoke(() -> Registry.convert(new Object[]{value.apply(machine, new ArgumentsImpl(params))}));
        }));
        userdata.set("unapply", function(args -> {
            Value value = (Value) args.checkuserdata(1, Value.class);
            Object[] params = toArray(ScalaClosureSupport.toSimpleJavaObjects(args, 2).iterator());
            return owner().invoke(() -> {
                value.unapply(machine, new ArgumentsImpl(params));
                return null;
            });
        }));
        userdata.set("call", function(args -> {
            Value value = (Value) args.checkuserdata(1, Value.class);
            Object[] params = toArray(ScalaClosureSupport.toSimpleJavaObjects(args, 2).iterator());
            return owner().invoke(() -> Registry.convert(value.call(machine, new ArgumentsImpl(params))));
        }));
        userdata.set("dispose", function(args -> {
            Value value = (Value) args.checkuserdata(1, Value.class);
            try {
                value.dispose(machine);
            } catch (Throwable error) {
                OpenComputers.log().warn("Error in dispose method of userdata of type " + value.getClass().getName(), error);
            }
            return LuaValue.NIL;
        }));
        userdata.set("methods", function(args -> {
            Value value = (Value) args.checkuserdata(1, Value.class);
            ArrayList<LuaValue> entries = new ArrayList<>();
            machine.methods(value).forEach((name, annotation) -> {
                entries.add(LuaValue.valueOf(name));
                entries.add(LuaValue.valueOf(annotation.direct()));
            });
            return LuaValue.tableOf(entries.toArray(LuaValue[]::new));
        }));
        userdata.set("invoke", function(args -> {
            Value value = (Value) args.checkuserdata(1, Value.class);
            String method = args.checkjstring(2);
            Object[] params = toArray(ScalaClosureSupport.toSimpleJavaObjects(args, 3).iterator());
            return owner().invoke(() -> {
                try {
                    return machine.invoke(value, method, params);
                } catch (Exception error) {
                    throw unchecked(error);
                }
            });
        }));
        userdata.set("doc", function(args -> {
            Value value = (Value) args.checkuserdata(1, Value.class);
            String method = args.checkjstring(2);
            return owner().documentation(() -> machine.methods(value).get(method).doc());
        }));
        lua().set("userdata", userdata);
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
