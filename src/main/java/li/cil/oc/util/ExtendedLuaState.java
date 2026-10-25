package li.cil.oc.util;

import li.cil.repack.com.naef.jnlua.LuaState;

/** Factory for the Java adapter around a Lua state. */
public final class ExtendedLuaState {
    private ExtendedLuaState() { }

    public static ExtendedLuaStateJava extendLuaState(LuaState state) {
        return new ExtendedLuaStateJava(state);
    }
}
