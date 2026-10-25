package li.cil.oc.server.machine.luac;

import com.google.common.base.Strings;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.server.machine.MachineState;
import li.cil.oc.util.ExtendedLuaState;
import li.cil.repack.com.naef.jnlua.*;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.IdentityHashMap;
import java.util.concurrent.Callable;

public abstract class NativeLuaArchitecture implements Architecture {
    private final Machine machine;
    private LuaState lua;
    private volatile int kernelMemory;
    private double ramScale = 1.0;
    private final PersistenceAPI persistence;
    private DataComponentHolder userdataLoadHolder;
    private MutableDataComponentHolder userdataSaveHolder;
    private final NativeLuaAPI[] apis;

    protected NativeLuaArchitecture(Machine machine) {
        this.machine = machine;
        persistence = new PersistenceAPI(this);
        apis = new NativeLuaAPI[] {
            new ComponentAPI(this), new ComputerAPI(this), new OSAPI(this), new SystemAPI(this),
            new UnicodeAPI(this), new UserdataAPI(this), persistence
        };
    }
    protected abstract LuaStateFactoryBase factory();
    public Machine machine() { return machine; }
    public LuaState lua() { return lua; }
    public int kernelMemory() { return kernelMemory; }
    public double ramScale() { return ramScale; }
    public DataComponentHolder userdataLoadHolder() { return userdataLoadHolder; }
    public MutableDataComponentHolder userdataSaveHolder() { return userdataSaveHolder; }

    private static String fingerprint(byte[] data) {
        byte[] digest;
        try { digest = MessageDigest.getInstance("SHA-256").digest(data); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) hex.append(String.format("%02x", value & 255));
        return hex.toString();
    }

    private void logPersistenceData(String operation, String name, byte[] data) {
        if (Settings.get().debugPersistence())
            OpenComputers.log().info("{} Lua persistence '{}' for {}: {} bytes, sha256={}, persistKey={}",
                operation, name, machine.node().address(), data.length, fingerprint(data), persistence.currentPersistKey());
    }

    public int invoke(Callable<Object[]> callback) {
        try {
            Object[] results = callback.call();
            lua.pushBoolean(true);
            if (results == null) return 1;
            for (Object result : results)
                ExtendedLuaState.extendLuaState(lua).pushValue(result, new IdentityHashMap<>());
            return 1 + results.length;
        } catch (Throwable error) {
            if (Settings.get().logLuaCallbackErrors() && !(error instanceof LimitReachedException))
                OpenComputers.log().warn("Exception in Lua callback.", error);
            if (error instanceof LimitReachedException) return 0;
            if (error instanceof IllegalArgumentException && error.getMessage() != null) {
                lua.pushBoolean(false); lua.pushString(error.getMessage()); return 2;
            }
            if (error.getMessage() != null) {
                lua.pushBoolean(true); lua.pushNil(); lua.pushString(error.getMessage());
                if (Settings.get().logLuaCallbackErrors()) {
                    StringBuilder stack = new StringBuilder();
                    for (StackTraceElement frame : error.getStackTrace()) stack.append(frame).append('\n');
                    lua.pushString(stack.toString());
                    return 4;
                }
                return 3;
            }
            String message = null;
            boolean argument = true;
            if (error instanceof IndexOutOfBoundsException) message = "index out of bounds";
            else if (error instanceof IllegalArgumentException) message = "bad argument";
            else if (error instanceof NoSuchMethodException) message = "no such method";
            else { argument = false;
                if (error instanceof FileNotFoundException) message = "file not found";
                else if (error instanceof SecurityException) message = "access denied";
                else if (error instanceof IOException) message = "i/o error";
                else if (error instanceof UnsupportedOperationException) { message = "unsupported operation"; argument = true; }
            }
            if (message == null) {
                OpenComputers.log().warn("Unexpected error in Lua callback.", error);
                message = "unknown error";
            }
            lua.pushBoolean(!argument);
            if (!argument) lua.pushNil();
            lua.pushString(message);
            return argument ? 2 : 3;
        }
    }

    public int documentation(Callable<String> callback) {
        try {
            String doc = callback.call();
            if (Strings.isNullOrEmpty(doc)) lua.pushNil(); else lua.pushString(doc);
            return 1;
        } catch (NoSuchMethodException error) {
            lua.pushNil(); lua.pushString("no such method"); return 2;
        } catch (Throwable error) {
            lua.pushNil(); lua.pushString(error.getMessage() == null ? error.toString() : error.getMessage()); return 2;
        }
    }

    @Override public boolean isInitialized() { return kernelMemory > 0; }
    @Override public boolean recomputeMemory(Iterable<ItemStack> components) {
        int memoryBytes = memoryInBytes(components);
        if (lua != null && Settings.get().limitMemory()) {
            lua.setTotalMemory(Integer.MAX_VALUE);
            if (kernelMemory > 0) lua.setTotalMemory(kernelMemory + (int) Math.ceil(memoryBytes * ramScale));
        }
        return memoryBytes > 0;
    }
    private int memoryInBytes(Iterable<ItemStack> components) {
        double total = 0;
        for (ItemStack stack : components) {
            var driver = Driver.driverFor(stack);
            if (driver instanceof Memory memory) total += memory.amount(stack) * 1024;
        }
        return Math.max(0, Math.min((int) total, Settings.get().maxTotalRam()));
    }

    @Override public void runSynchronized() {
        assert lua.getTop() == 2 && lua.isThread(1) && lua.isFunction(2);
        try { lua.call(0, 1); lua.checkType(2, LuaType.TABLE); }
        catch (LuaMemoryAllocationException error) { throw new OutOfMemoryError("not enough memory"); }
    }

    @Override public ExecutionResult runThreaded(boolean synchronizedReturn) {
        try {
            assert lua.isThread(1);
            int results;
            if (synchronizedReturn) {
                assert lua.getTop() == 2 && lua.isTable(2);
                results = lua.resume(1, 1);
            } else if (kernelMemory == 0) {
                if (lua.resume(1, 0) > 0) results = 0;
                else {
                    lua.gc(LuaState.GcAction.COLLECT, 0);
                    kernelMemory = Math.max(lua.getTotalMemory() - lua.getFreeMemory(), 1);
                    recomputeMemory(machine.host().internalComponents());
                    lua.pushInteger(0);
                    results = 1;
                }
            } else {
                Signal signal = machine.popSignal();
                if (signal == null) results = lua.resume(1, 0);
                else {
                    lua.pushString(signal.name());
                    for (Object argument : signal.args())
                        ExtendedLuaState.extendLuaState(lua).pushValue(argument, new IdentityHashMap<>());
                    results = lua.resume(1, 1 + signal.args().length);
                }
            }
            if (lua.status(1) == LuaState.YIELD) {
                if (results == 1 && lua.isFunction(2)) return new ExecutionResult.SynchronizedCall();
                if (results == 1 && lua.isBoolean(2)) return new ExecutionResult.Shutdown(lua.toBoolean(2));
                int ticks = results == 1 && lua.isNumber(2) ? (int) (lua.toNumber(2) * 20) : Integer.MAX_VALUE;
                lua.pop(results);
                return new ExecutionResult.Sleep(ticks);
            }
            assert lua.isThread(1);
            if (!lua.isBoolean(2) || !(lua.isString(3) || lua.isNoneOrNil(3)))
                OpenComputers.log().warn("Kernel returned unexpected results.");
            if (lua.toBoolean(2)) {
                OpenComputers.log().warn("Kernel stopped unexpectedly.");
                return new ExecutionResult.Shutdown(false);
            }
            if (Settings.get().limitMemory()) lua.setTotalMemory(Integer.MAX_VALUE);
            String error = lua.isJavaObjectRaw(3) ? lua.toJavaObjectRaw(3).toString() : lua.toString(3);
            return new ExecutionResult.Error(error == null ? "unknown error" : error);
        } catch (LuaRuntimeException error) {
            OpenComputers.log().warn("Kernel crashed. This is a bug!\n" + error + luaStack(error));
            return new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it");
        } catch (LuaGcMetamethodException error) {
            return new ExecutionResult.Error(error.getMessage() == null
                ? "kernel panic:\nerror in garbage collection metamethod" : "kernel panic:\n" + error.getMessage());
        } catch (LuaMemoryAllocationException error) { return new ExecutionResult.Error("not enough memory"); }
        catch (java.lang.Error error) {
            if ("not enough memory".equals(error.getMessage())) return new ExecutionResult.Error("not enough memory");
            throw error;
        }
    }

    @Override public void onSignal() { }
    @Override public boolean initialize() {
        var created = factory().createState();
        if (created.isEmpty()) {
            lua = null;
            machine.crash("native libraries not available");
            return false;
        }
        lua = created.get();
        ramScale = lua.getPointerWidth() >= 8 ? Settings.get().ramScaleFor64Bit() : 1.0;
        for (NativeLuaAPI api : apis) api.initialize();
        try {
            lua.load(li.cil.oc.server.machine.Machine.class.getResourceAsStream(Settings.scriptPath() + "machine.lua"),
                "=machine", "t");
        } catch (IOException error) { throw new RuntimeException(error); }
        lua.newThread();
        return true;
    }
    @Override public void onConnect() { }
    @Override public void close() {
        if (lua != null) {
            if (Settings.get().limitMemory()) lua.setTotalMemory(Integer.MAX_VALUE);
            lua.close();
        }
        lua = null;
        kernelMemory = 0;
    }

    private java.util.Deque<MachineState> state() {
        return ((li.cil.oc.server.machine.Machine) machine).state;
    }

    private static String luaStack(LuaRuntimeException error) {
        String[] frames = java.util.Arrays.stream(error.getLuaStackTrace()).map(Object::toString).toArray(String[]::new);
        return frames.length == 0 ? "" : "\tat " + String.join("\n\tat ", frames);
    }

    @Override public void loadData(CompoundTag nbt) {
        if (!machine.isRunning()) return;
        if (Settings.get().limitMemory()) lua.setTotalMemory(Integer.MAX_VALUE);
        try {
            lua.setTop(0);
            String kernelName = machine.node().address() + "_kernel";
            byte[] kernelData = SaveHandler.load(nbt, kernelName);
            logPersistenceData("Loading", kernelName, kernelData);
            persistence.unpersist(kernelData);
            if (!lua.isThread(1)) throw new LuaRuntimeException("Invalid kernel.");
            if (state().contains(MachineState.SynchronizedCall) || state().contains(MachineState.SynchronizedReturn)) {
                String stackName = machine.node().address() + "_stack";
                byte[] stackData = SaveHandler.load(nbt, stackName);
                logPersistenceData("Loading", stackName, stackData);
                persistence.unpersist(stackData);
                if (!(state().contains(MachineState.SynchronizedCall) ? lua.isFunction(2) : lua.isTable(2)))
                    throw new LuaRuntimeException("Invalid stack.");
            }
            kernelMemory = (int) (nbt.getInt("kernelMemory") * ramScale);
            for (NativeLuaAPI api : apis) api.loadData(nbt);
            try { lua.gc(LuaState.GcAction.COLLECT, 0); }
            catch (Throwable error) {
                OpenComputers.log().warn("Error cleaning up loaded computer @ {}. This either means the server is badly overloaded or a user created an evil __gc method, accidentally or not.", machine.host().machinePosition());
                machine.crash("error in garbage collector, most likely __gc method timed out");
            }
        } catch (LuaRuntimeException error) {
            throw NativeLuaArchitecture.<RuntimeException>unchecked(
                new Exception(error + luaStack(error), error));
        }
        recomputeMemory(machine.host().internalComponents());
    }

    @Override public void loadData(DataComponentHolder holder, CompoundTag nbt) {
        userdataLoadHolder = holder;
        try { loadData(nbt); } finally { userdataLoadHolder = null; }
    }

    @Override public void saveData(CompoundTag nbt) {
        if (lua == null) return;
        if (Settings.get().limitMemory()) lua.setTotalMemory(Integer.MAX_VALUE);
        try {
            assert lua.isThread(1);
            String kernelName = machine.node().address() + "_kernel";
            byte[] kernelData = persistence.persist(1);
            logPersistenceData("Saving", kernelName, kernelData);
            SaveHandler.scheduleSave(machine.host(), nbt, kernelName, kernelData);
            if (state().contains(MachineState.SynchronizedCall) || state().contains(MachineState.SynchronizedReturn)) {
                assert state().contains(MachineState.SynchronizedCall) ? lua.isFunction(2) : lua.isTable(2);
                String stackName = machine.node().address() + "_stack";
                byte[] stackData = persistence.persist(2);
                logPersistenceData("Saving", stackName, stackData);
                SaveHandler.scheduleSave(machine.host(), nbt, stackName, stackData);
            }
            nbt.putInt("kernelMemory", (int) Math.ceil(kernelMemory / ramScale));
            for (NativeLuaAPI api : apis) api.saveData(nbt);
            try { lua.gc(LuaState.GcAction.COLLECT, 0); }
            catch (Throwable error) {
                OpenComputers.log().warn("Error cleaning up loaded computer @ {}. This either means the server is badly overloaded or a user created an evil __gc method, accidentally or not.", machine.host().machinePosition());
                machine.crash("error in garbage collector, most likely __gc method timed out");
            }
        } catch (LuaRuntimeException error) {
            OpenComputers.log().warn("Could not persist computer @ {}.\n{}{}", machine.host().machinePosition(), error, luaStack(error));
            nbt.remove("state");
        } catch (LuaGcMetamethodException error) {
            OpenComputers.log().warn("Could not persist computer @ {}.\n{}", machine.host().machinePosition(), error);
            nbt.remove("state");
        }
        recomputeMemory(machine.host().internalComponents());
    }

    @Override public void saveData(MutableDataComponentHolder holder, CompoundTag nbt) {
        userdataSaveHolder = holder;
        try { saveData(nbt); } finally { userdataSaveHolder = null; }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException unchecked(Throwable error) throws E {
        throw (E) error;
    }
}
