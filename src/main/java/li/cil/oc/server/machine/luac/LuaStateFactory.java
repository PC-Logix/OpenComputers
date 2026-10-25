package li.cil.oc.server.machine.luac;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.repack.com.naef.jnlua.LuaState;
import net.minecraft.world.item.ItemStack;

/** Native Lua factory registry and Lua-version-specific library setup. */
public final class LuaStateFactory {
    public static final LuaStateFactoryBase Lua52 = new LuaStateFactoryBase() {
        @Override protected String version() { return "52"; }
        @Override protected LuaState create(Integer maxMemory) {
            return maxMemory == null ? new LuaState() : new LuaState(maxMemory.intValue());
        }
        @Override protected void openLibs(LuaState state) {
            state.openLib(LuaState.Library.BASE);
            state.openLib(LuaState.Library.BIT32);
            state.openLib(LuaState.Library.COROUTINE);
            state.openLib(LuaState.Library.DEBUG);
            state.openLib(LuaState.Library.ERIS);
            state.openLib(LuaState.Library.MATH);
            state.openLib(LuaState.Library.STRING);
            state.openLib(LuaState.Library.TABLE);
            state.pop(8);
        }
    };

    public static final LuaStateFactoryBase Lua53 = new LuaStateFactoryBase() {
        @Override protected String version() { return "53"; }
        @Override protected LuaState create(Integer maxMemory) {
            return maxMemory == null ? new li.cil.repack.com.naef.jnlua.LuaStateFiveThree() : new li.cil.repack.com.naef.jnlua.LuaStateFiveThree(maxMemory.intValue());
        }
        @Override protected void openLibs(LuaState state) {
            state.openLib(LuaState.Library.BASE);
            state.openLib(LuaState.Library.COROUTINE);
            state.openLib(LuaState.Library.DEBUG);
            state.openLib(LuaState.Library.ERIS);
            state.openLib(LuaState.Library.MATH);
            state.openLib(LuaState.Library.STRING);
            state.openLib(LuaState.Library.TABLE);
            state.openLib(LuaState.Library.UTF8);
            state.pop(8);
        }
    };

    public static final LuaStateFactoryBase Lua54 = new LuaStateFactoryBase() {
        @Override protected String version() { return "54"; }
        @Override protected LuaState create(Integer maxMemory) {
            return maxMemory == null ? new li.cil.repack.com.naef.jnlua.LuaStateFiveFour() : new li.cil.repack.com.naef.jnlua.LuaStateFiveFour(maxMemory.intValue());
        }
        @Override protected void openLibs(LuaState state) {
            state.openLib(LuaState.Library.BASE);
            state.openLib(LuaState.Library.COROUTINE);
            state.openLib(LuaState.Library.DEBUG);
            state.openLib(LuaState.Library.ERIS);
            state.openLib(LuaState.Library.MATH);
            state.openLib(LuaState.Library.STRING);
            state.openLib(LuaState.Library.TABLE);
            state.openLib(LuaState.Library.UTF8);
            state.pop(8);
        }
    };

    private LuaStateFactory() { }

    public static boolean isAvailable() {
        return Lua52.isAvailable() || Lua53.isAvailable() || Lua54.isAvailable();
    }

    public static boolean luajRequested() {
        return Settings.get().forceLuaJ() || Settings.get().registerLuaJArchitecture();
    }

    public static boolean includeLuaJ() { return !isAvailable() || luajRequested(); }

    public static boolean include52() { return Lua52.isAvailable() && !Settings.get().forceLuaJ(); }

    public static boolean include53() {
        return Lua53.isAvailable() && Settings.get().enableLua53() && !Settings.get().forceLuaJ();
    }

    public static boolean include54() {
        return Lua54.isAvailable() && Settings.get().enableLua54() && !Settings.get().forceLuaJ();
    }

    public static boolean default53() { return include53() && Settings.get().defaultLua53(); }

    public static ItemStack setDefaultArch(ItemStack stack) {
        if (default53()) {
            Object driver = Driver.driverFor(stack);
            if (driver instanceof MutableProcessor processor) {
                processor.setArchitecture(stack, NativeLua53Architecture.class);
            }
        }
        return stack;
    }
}
