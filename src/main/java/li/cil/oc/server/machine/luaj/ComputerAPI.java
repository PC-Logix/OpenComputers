package li.cil.oc.server.machine.luaj;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Machine;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.Connector;
import li.cil.oc.util.ScalaClosureSupport;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;
import li.cil.repack.org.luaj.vm2.lib.VarArgFunction;
import net.minecraft.world.item.ItemStack;
import scala.collection.Iterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class ComputerAPI extends LuaJAPI {
    public ComputerAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        LuaValue computer = LuaValue.tableOf();
        computer.set("realTime", function(args -> LuaValue.valueOf(System.currentTimeMillis() / 1000.0)));
        computer.set("uptime", function(args -> LuaValue.valueOf(machine.upTime())));
        computer.set("address", function(args -> {
            String address = node().address();
            return address == null ? LuaValue.NIL : LuaValue.valueOf(address);
        }));
        computer.set("freeMemory", function(args -> LuaValue.valueOf(owner().memory() / 2)));
        computer.set("totalMemory", function(args -> LuaValue.valueOf(owner().memory())));
        computer.set("pushSignal", function(args -> {
            String name = args.checkjstring(1);
            Object[] values = toArray(ScalaClosureSupport.toSimpleJavaObjects(args, 2).iterator());
            return LuaValue.valueOf(machine.signal(name, values));
        }));
        computer.set("tmpAddress", function(args -> {
            String address = machine.tmpAddress();
            return address == null ? LuaValue.NIL : LuaValue.valueOf(address);
        }));
        computer.set("users", function(args -> {
            String[] users = machine.users();
            LuaValue[] values = new LuaValue[users.length];
            for (int i = 0; i < users.length; i++) values[i] = LuaValue.valueOf(users[i]);
            return LuaValue.varargsOf(values);
        }));
        computer.set("addUser", function(args -> {
            try {
                machine.addUser(args.checkjstring(1));
                return LuaValue.TRUE;
            } catch (Exception error) {
                throw unchecked(error);
            }
        }));
        computer.set("removeUser", function(args -> LuaValue.valueOf(machine.removeUser(args.checkjstring(1)))));
        computer.set("energy", function(args -> LuaValue.valueOf(Settings.get().ignorePower()
                ? Double.POSITIVE_INFINITY : ((Connector) node()).globalBuffer())));
        computer.set("maxEnergy", function(args -> LuaValue.valueOf(((Connector) node()).globalBufferSize())));
        computer.set("getArchitectures", function(args -> {
            Collection<Class<? extends Architecture>> architectures = architectures();
            LuaValue[] names = architectures.stream().map(Machine::getArchitectureName)
                    .map(LuaValue::valueOf).toArray(LuaValue[]::new);
            return names.length == 0 ? LuaValue.tableOf() : LuaValue.listOf(names);
        }));
        computer.set("getArchitecture", function(args -> {
            for (ItemStack stack : machine.host().internalComponents()) {
                if (Driver.driverFor(stack) instanceof Processor processor) {
                    return LuaValue.valueOf(Machine.getArchitectureName(processor.architecture(stack)));
                }
            }
            return LuaValue.NONE;
        }));
        computer.set("setArchitecture", function(args -> {
            String name = args.checkjstring(1);
            for (ItemStack stack : machine.host().internalComponents()) {
                if (Driver.driverFor(stack) instanceof MutableProcessor processor) {
                    for (Class<? extends Architecture> architecture : processor.allArchitectures()) {
                        if (Machine.getArchitectureName(architecture).equals(name)) {
                            boolean changed = architecture != processor.architecture(stack);
                            if (changed) processor.setArchitecture(stack, architecture);
                            return LuaValue.valueOf(changed);
                        }
                    }
                    return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("unknown architecture"));
                }
            }
            return LuaValue.NONE;
        }));
        lua().set("computer", computer);
    }

    private Collection<Class<? extends Architecture>> architectures() {
        for (ItemStack stack : machine.host().internalComponents()) {
            var driver = Driver.driverFor(stack);
            if (driver instanceof MutableProcessor processor) return processor.allArchitectures();
            if (driver instanceof Processor processor) return List.of(processor.architecture(stack));
        }
        return List.of();
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
