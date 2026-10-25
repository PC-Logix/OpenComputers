package li.cil.oc.server.machine.luac;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Machine;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.Connector;
import li.cil.oc.util.ExtendedLuaState;
import li.cil.repack.com.naef.jnlua.JavaFunction;
import li.cil.repack.com.naef.jnlua.LuaState;
import net.minecraft.world.item.ItemStack;
import scala.collection.Iterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ComputerAPI extends NativeLuaAPI {
    public ComputerAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        lua().newTable();
        add("realTime", state -> {
            state.pushNumber(System.currentTimeMillis() / 1000.0);
            return 1;
        });
        add("uptime", state -> {
            state.pushNumber(machine.upTime());
            return 1;
        });
        add("address", state -> {
            String address = node().address();
            if (address == null) state.pushNil();
            else state.pushString(address);
            return 1;
        });
        add("freeMemory", state -> {
            state.pushInteger((int) (Math.min(state.getFreeMemory(),
                    state.getTotalMemory() - owner().kernelMemory()) / owner().ramScale()));
            return 1;
        });
        add("totalMemory", state -> {
            state.pushInteger((int) ((state.getTotalMemory() - owner().kernelMemory()) / owner().ramScale()));
            return 1;
        });
        add("pushSignal", state -> {
            String name = state.checkString(1);
            Object[] args = toArray(ExtendedLuaState.extendLuaState(state).toSimpleJavaObjects(2).iterator());
            state.pushBoolean(machine.signal(name, args));
            return 1;
        });
        add("tmpAddress", state -> {
            String address = machine.tmpAddress();
            if (address == null) state.pushNil();
            else state.pushString(address);
            return 1;
        });
        add("users", state -> {
            String[] users = machine.users();
            for (String user : users) state.pushString(user);
            return users.length;
        });
        add("addUser", state -> {
            try {
                machine.addUser(state.checkString(1));
                state.pushBoolean(true);
                return 1;
            } catch (Throwable error) {
                state.pushNil();
                state.pushString(error.getMessage() != null ? error.getMessage() : error.toString());
                return 2;
            }
        });
        add("removeUser", state -> {
            state.pushBoolean(machine.removeUser(state.checkString(1)));
            return 1;
        });
        add("energy", state -> {
            state.pushNumber(Settings.get().ignorePower() ? Double.POSITIVE_INFINITY : ((Connector) node()).globalBuffer());
            return 1;
        });
        add("maxEnergy", state -> {
            state.pushNumber(((Connector) node()).globalBufferSize());
            return 1;
        });
        add("getArchitectures", state -> {
            Collection<Class<? extends Architecture>> architectures = architectures();
            state.newTable();
            int index = 1;
            for (Class<? extends Architecture> architecture : architectures) {
                state.pushInteger(index++);
                state.pushString(Machine.getArchitectureName(architecture));
                state.rawSet(-3);
            }
            return 1;
        });
        add("getArchitecture", state -> {
            for (ItemStack stack : machine.host().internalComponents()) {
                if (Driver.driverFor(stack) instanceof Processor processor) {
                    state.pushString(Machine.getArchitectureName(processor.architecture(stack)));
                    return 1;
                }
            }
            return 0;
        });
        add("setArchitecture", state -> {
            String name = state.checkString(1);
            for (ItemStack stack : machine.host().internalComponents()) {
                if (Driver.driverFor(stack) instanceof MutableProcessor processor) {
                    for (Class<? extends Architecture> architecture : processor.allArchitectures()) {
                        if (Machine.getArchitectureName(architecture).equals(name)) {
                            boolean changed = architecture != processor.architecture(stack);
                            if (changed) processor.setArchitecture(stack, architecture);
                            state.pushBoolean(changed);
                            return 1;
                        }
                    }
                    state.pushNil();
                    state.pushString("unknown architecture");
                    return 2;
                }
            }
            return 0;
        });
        lua().setGlobal("computer");
    }

    private Collection<Class<? extends Architecture>> architectures() {
        for (ItemStack stack : machine.host().internalComponents()) {
            var driver = Driver.driverFor(stack);
            if (driver instanceof MutableProcessor processor) return processor.allArchitectures();
            if (driver instanceof Processor processor) return List.of(processor.architecture(stack));
        }
        return List.of();
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
}
