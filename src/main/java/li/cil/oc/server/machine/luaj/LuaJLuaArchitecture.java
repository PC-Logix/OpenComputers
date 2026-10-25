package li.cil.oc.server.machine.luaj;

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
import li.cil.oc.util.ScalaClosureSupport;
import li.cil.repack.org.luaj.vm2.Globals;
import li.cil.repack.org.luaj.vm2.LuaError;
import li.cil.repack.org.luaj.vm2.LuaFunction;
import li.cil.repack.org.luaj.vm2.LuaThread;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;
import li.cil.repack.org.luaj.vm2.lib.jse.JsePlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Supplier;

@Architecture.Name("LuaJ")
public class LuaJLuaArchitecture implements Architecture {
    private final Machine machine;
    private Globals lua;
    private LuaThread thread;
    private LuaFunction synchronizedCall;
    private LuaValue synchronizedResult;
    private boolean doneWithInitRun;
    private volatile int memory;
    private final LuaJAPI[] apis;

    public LuaJLuaArchitecture(Machine machine) {
        this.machine = machine;
        this.apis = new LuaJAPI[]{
            new ComponentAPI(this),
            new ComputerAPI(this),
            new OSAPI(this),
            new SystemAPI(this),
            new UnicodeAPI(this),
            new UserdataAPI(this)
        };
    }

    public Machine machine() { return machine; }
    public Globals lua() { return lua; }
    public int memory() { return memory; }

    public Varargs invoke(Supplier<Object[]> callback) {
        try {
            Object[] results = callback.get();
            if (results == null) return LuaValue.TRUE;
            LuaValue[] values = new LuaValue[results.length + 1];
            values[0] = LuaValue.TRUE;
            for (int i = 0; i < results.length; i++) {
                values[i + 1] = ScalaClosureSupport.toLuaValue(results[i]);
            }
            return LuaValue.varargsOf(values);
        }
        catch (Throwable error) {
            if (Settings.get().logLuaCallbackErrors() && !(error instanceof LimitReachedException)) {
                OpenComputers.log().warn("Exception in Lua callback.", error);
            }
            if (error instanceof LimitReachedException) return LuaValue.NONE;
            if (error instanceof IllegalArgumentException && error.getMessage() != null) {
                return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf(error.getMessage()));
            }
            if (error.getMessage() != null) {
                return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf(error.getMessage()));
            }
            if (error instanceof IndexOutOfBoundsException) {
                return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("index out of bounds"));
            }
            if (error instanceof IllegalArgumentException) {
                return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("bad argument"));
            }
            if (error instanceof NoSuchMethodException) {
                return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("no such method"));
            }
            if (error instanceof FileNotFoundException) {
                return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("file not found"));
            }
            if (error instanceof SecurityException) {
                return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("access denied"));
            }
            if (error instanceof IOException) {
                return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("i/o error"));
            }
            OpenComputers.log().warn("Unexpected error in Lua callback.", error);
            return LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("unknown error"));
        }
    }

    public Varargs documentation(Supplier<String> callback) {
        try {
            String doc = callback.get();
            return Strings.isNullOrEmpty(doc) ? LuaValue.NIL : LuaValue.valueOf(doc);
        }
        catch (Throwable error) {
            if (error instanceof NoSuchMethodException) {
                return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such method"));
            }
            String message = error.getMessage() != null ? error.getMessage() : error.toString();
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(message));
        }
    }

    @Override
    public boolean isInitialized() {
        return doneWithInitRun;
    }

    @Override
    public boolean recomputeMemory(Iterable<ItemStack> components) {
        memory = memoryInBytes(components);
        return memory > 0;
    }

    private int memoryInBytes(Iterable<ItemStack> components) {
        double total = 0;
        for (ItemStack stack : components) {
            Object driver = Driver.driverFor(stack);
            if (driver instanceof Memory memoryDriver) total += memoryDriver.amount(stack) * 1024;
        }
        return Math.max(0, Math.min((int) total, Settings.get().maxTotalRam()));
    }

    @Override
    public void runSynchronized() {
        synchronizedResult = synchronizedCall.call();
        synchronizedCall = null;
    }

    @Override
    public ExecutionResult runThreaded(boolean isSynchronizedReturn) {
        try {
            Varargs results;
            if (isSynchronizedReturn) {
                results = thread.resume(synchronizedResult);
                synchronizedResult = null;
            }
            else if (!doneWithInitRun) {
                results = thread.resume(LuaValue.NONE);
                doneWithInitRun = true;
                if (results.narg() == 1) {
                    results = LuaValue.varargsOf(LuaValue.TRUE, LuaValue.valueOf(0));
                }
            }
            else {
                Signal signal = machine.popSignal();
                if (signal != null) {
                    Object[] arguments = signal.args();
                    LuaValue[] values = new LuaValue[arguments.length + 1];
                    values[0] = LuaValue.valueOf(signal.name());
                    for (int i = 0; i < arguments.length; i++) {
                        values[i + 1] = ScalaClosureSupport.toLuaValue(arguments[i]);
                    }
                    results = thread.resume(LuaValue.varargsOf(values));
                }
                else {
                    results = thread.resume(LuaValue.NONE);
                }
            }

            if (thread.state.status == LuaThread.STATUS_SUSPENDED) {
                if (results.narg() == 2 && results.isfunction(2)) {
                    synchronizedCall = results.checkfunction(2);
                    return new ExecutionResult.SynchronizedCall();
                }
                if (results.narg() == 2 && results.type(2) == LuaValue.TBOOLEAN) {
                    return new ExecutionResult.Shutdown(results.toboolean(2));
                }
                int ticks = results.narg() == 2 && results.isnumber(2)
                    ? (int) (results.todouble(2) * 20)
                    : Integer.MAX_VALUE;
                return new ExecutionResult.Sleep(ticks);
            }

            boolean innerError = results.type(2) == LuaValue.TBOOLEAN
                && (results.isstring(3) || results.isnoneornil(3));
            boolean outerError = results.isstring(2) || results.isnoneornil(2);
            if (results.type(1) != LuaValue.TBOOLEAN || !(innerError || outerError)) {
                OpenComputers.log().warn("Kernel returned unexpected results.");
                OpenComputers.log().warn("Returned: {}", results);
            }
            if ((outerError && results.toboolean(1)) || (innerError && results.toboolean(2))) {
                OpenComputers.log().warn("Kernel stopped unexpectedly.");
                return new ExecutionResult.Shutdown(false);
            }
            String error;
            if (innerError) error = results.isuserdata(3) ? results.touserdata(3).toString() : results.tojstring(3);
            else error = results.isuserdata(2) ? results.touserdata(2).toString() : results.tojstring(2);
            return new ExecutionResult.Error(error != null ? error : "unknown error");
        }
        catch (LuaError error) {
            OpenComputers.log().warn("Kernel crashed. This is a bug!", error);
            return new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it");
        }
        catch (Throwable error) {
            OpenComputers.log().warn("Unexpected error in kernel. This is a bug!", error);
            return new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it");
        }
    }

    @Override
    public void onSignal() {}

    @Override
    public boolean initialize() {
        lua = JsePlatform.debugGlobals();
        lua.set("package", LuaValue.NIL);
        lua.set("require", LuaValue.NIL);
        lua.set("io", LuaValue.NIL);
        lua.set("os", LuaValue.NIL);
        lua.set("luajava", LuaValue.NIL);
        lua.set("dofile", LuaValue.NIL);
        lua.set("loadfile", LuaValue.NIL);

        for (LuaJAPI api : apis) api.initialize();
        recomputeMemory(machine.host().internalComponents());

        LuaValue kernel = lua.load(Machine.class.getResourceAsStream(Settings.scriptPath() + "machine.lua"), "=machine", "t", lua);
        thread = new LuaThread(lua, kernel);
        return true;
    }

    @Override
    public void onConnect() {}

    @Override
    public void close() {
        lua = null;
        thread = null;
        synchronizedCall = null;
        synchronizedResult = null;
        doneWithInitRun = false;
    }

    @Override
    public void loadData(CompoundTag nbt) {
        if (machine.isRunning()) {
            machine.stop();
            machine.start();
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {}
}
