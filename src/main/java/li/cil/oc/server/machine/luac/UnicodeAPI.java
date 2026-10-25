package li.cil.oc.server.machine.luac;

import li.cil.oc.util.ExtendedUnicodeHelper;
import li.cil.oc.util.FontUtils;
import li.cil.repack.com.naef.jnlua.JavaFunction;
import li.cil.repack.com.naef.jnlua.LuaState;

public class UnicodeAPI extends NativeLuaAPI {
    public UnicodeAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        lua().newTable();
        add("char", state -> {
            StringBuilder builder = new StringBuilder();
            for (int index = 1; index <= state.getTop(); index++) builder.appendCodePoint(state.checkInt32(index));
            state.pushString(builder.toString());
            return 1;
        });
        add("len", state -> {
            state.pushInteger(ExtendedUnicodeHelper.length(state.checkString(1)));
            return 1;
        });
        add("lower", state -> {
            state.pushString(state.checkString(1).toLowerCase());
            return 1;
        });
        add("reverse", state -> {
            state.pushString(ExtendedUnicodeHelper.reverse(state.checkString(1)));
            return 1;
        });
        add("sub", state -> {
            String value = state.checkString(1);
            int length = ExtendedUnicodeHelper.length(value);
            int startIndex = state.checkInt32(2);
            int start = startIndex < 0
                ? value.offsetByCodePoints(value.length(), Math.max(startIndex, -length))
                : value.offsetByCodePoints(0, Math.min(Math.max(startIndex - 1, 0), length));
            int end;
            if (state.getTop() > 2) {
                int endIndex = state.checkInt32(3);
                end = endIndex < 0
                    ? value.offsetByCodePoints(value.length(), Math.max(endIndex + 1, -length))
                    : value.offsetByCodePoints(0, Math.min(endIndex, length));
            } else end = value.length();
            state.pushString(end <= start ? "" : value.substring(start, end));
            return 1;
        });
        add("upper", state -> {
            state.pushString(state.checkString(1).toUpperCase());
            return 1;
        });
        add("isWide", state -> {
            state.pushBoolean(FontUtils.wcwidth(state.checkString(1).codePointAt(0)) > 1);
            return 1;
        });
        add("charWidth", state -> {
            state.pushInteger(FontUtils.wcwidth(state.checkString(1).codePointAt(0)));
            return 1;
        });
        add("wlen", state -> {
            state.pushInteger(state.checkString(1).codePoints().map(FontUtils::wcwidth).sum());
            return 1;
        });
        add("wtrunc", state -> {
            state.pushString(FontUtils.wtrunc(state.checkString(1), state.checkInteger(2)));
            return 1;
        });
        lua().setGlobal("unicode");
    }

    private void add(String name, JavaFunction function) {
        lua().pushJavaFunction(function);
        lua().setField(-2, name);
    }
}
