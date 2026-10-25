package li.cil.oc.server.machine.luac;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.machine.Value;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.server.machine.ArgumentsImpl;
import li.cil.oc.util.ExtendedLuaState;
import li.cil.repack.com.naef.jnlua.JavaFunction;
import li.cil.repack.com.naef.jnlua.LuaState;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import scala.collection.Iterator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class UserdataAPI extends NativeLuaAPI {
    public UserdataAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        lua().newTable();
        add("save", state -> {
            Value value = (Value) state.toJavaObjectRaw(1);
            var provider = ServerLifecycleHooks.getCurrentServer().registryAccess();
            SavedValue saved = saveValue(value, owner().userdataSaveHolder(), provider);
            state.pushString(saved.className());
            state.pushByteArray(saved.data());
            return 2;
        });
        add("load", state -> {
            try {
                String className = state.toString(1);
                byte[] data = state.toByteArray(2);
                var provider = ServerLifecycleHooks.getCurrentServer().registryAccess();
                Value value = loadValue(className, data, owner().userdataLoadHolder(), provider);
                state.pushJavaObjectRaw(value);
                return 1;
            } catch (Throwable error) {
                OpenComputers.log().warn("Error in userdata load function.", error);
                throw unchecked(error);
            }
        });
        add("apply", state -> {
            Value value = (Value) state.toJavaObjectRaw(1);
            Object[] args = toArray(ExtendedLuaState.extendLuaState(state).toSimpleJavaObjects(2).iterator());
            return owner().invoke(() -> Registry.convert(new Object[]{value.apply(machine, new ArgumentsImpl(args))}));
        });
        add("unapply", state -> {
            Value value = (Value) state.toJavaObjectRaw(1);
            Object[] args = toArray(ExtendedLuaState.extendLuaState(state).toSimpleJavaObjects(2).iterator());
            return owner().invoke(() -> {
                value.unapply(machine, new ArgumentsImpl(args));
                return null;
            });
        });
        add("call", state -> {
            Value value = (Value) state.toJavaObjectRaw(1);
            Object[] args = toArray(ExtendedLuaState.extendLuaState(state).toSimpleJavaObjects(2).iterator());
            return owner().invoke(() -> Registry.convert(value.call(machine, new ArgumentsImpl(args))));
        });
        add("dispose", state -> {
            Value value = (Value) state.toJavaObjectRaw(1);
            try {
                value.dispose(machine);
            } catch (Throwable error) {
                OpenComputers.log().warn("Error in dispose method of userdata of type " + value.getClass().getName(), error);
            }
            return 0;
        });
        add("methods", state -> {
            Value value = (Value) state.toJavaObjectRaw(1);
            state.newTable();
            machine.methods(value).forEach((name, annotation) -> {
                state.pushString(name);
                state.pushBoolean(annotation.direct());
                state.rawSet(-3);
            });
            return 1;
        });
        add("invoke", state -> {
            Value value = (Value) state.toJavaObjectRaw(1);
            String method = state.checkString(2);
            Object[] args = toArray(ExtendedLuaState.extendLuaState(state).toSimpleJavaObjects(3).iterator());
            return owner().invoke(() -> {
                try {
                    return machine.invoke(value, method, args);
                } catch (Exception error) {
                    throw unchecked(error);
                }
            });
        });
        add("doc", state -> {
            Value value = (Value) state.toJavaObjectRaw(1);
            String method = state.checkString(2);
            return owner().documentation(() -> machine.methods(value).get(method).doc());
        });
        lua().setGlobal("userdata");
    }

    public static SavedValue saveValue(Value value, MutableDataComponentHolder holder, HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        value.saveData(holder, tag, provider);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            NbtIo.write(tag, output);
        } catch (IOException error) {
            throw unchecked(error);
        }
        return new SavedValue(value.getClass().getName(), bytes.toByteArray());
    }

    public static Value loadValue(String className, byte[] data, DataComponentHolder holder, HolderLookup.Provider provider)
            throws ReflectiveOperationException, IOException {
        Value value = (Value) Class.forName(className).getDeclaredConstructor().newInstance();
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            value.loadData(holder, NbtIo.read(input), provider);
        }
        return value;
    }

    public record SavedValue(String className, byte[] data) {
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
