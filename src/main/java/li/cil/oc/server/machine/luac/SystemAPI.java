package li.cil.oc.server.machine.luac;

import li.cil.oc.Settings;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaType;

public class SystemAPI extends NativeLuaAPI {
    public SystemAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        lua().pushJavaFunction(state -> {
            StringBuilder message = new StringBuilder();
            for (int index = 1; index <= state.getTop(); index++) {
                if (index > 1) message.append("  ");
                message.append(printableValue(state, index));
            }
            System.out.println(message);
            return 0;
        });
        lua().setGlobal("print");

        lua().newTable();
        lua().pushJavaFunction(state -> {
            state.pushBoolean(Settings.get().allowBytecode());
            return 1;
        });
        lua().setField(-2, "allowBytecode");

        lua().pushJavaFunction(state -> {
            state.pushBoolean(Settings.get().allowGC());
            return 1;
        });
        lua().setField(-2, "allowGC");

        lua().pushJavaFunction(state -> {
            state.pushNumber(Settings.get().timeout());
            return 1;
        });
        lua().setField(-2, "timeout");
        lua().setGlobal("system");
    }

    private static String printableValue(LuaState state, int index) {
        LuaType type = state.type(index);
        return switch (type) {
            case NIL -> "nil";
            case BOOLEAN -> Boolean.toString(state.toBoolean(index));
            case NUMBER -> state.isInteger(index)
                ? Long.toString(state.toInteger(index)) : Double.toString(state.toNumber(index));
            case STRING -> state.toString(index);
            case TABLE -> "table";
            case FUNCTION -> "function";
            case THREAD -> "thread";
            case LIGHTUSERDATA, USERDATA -> "userdata";
        };
    }
}
