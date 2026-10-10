package li.cil.oc.server.machine.luaj;

import li.cil.oc.util.ExtendedUnicodeHelper;
import li.cil.oc.util.FontUtils;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;
import li.cil.repack.org.luaj.vm2.lib.VarArgFunction;

import java.util.function.Function;

public class UnicodeAPI extends LuaJAPI {
    public UnicodeAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        LuaValue unicode = LuaValue.tableOf();
        unicode.set("lower", function(args -> LuaValue.valueOf(args.checkjstring(1).toLowerCase())));
        unicode.set("upper", function(args -> LuaValue.valueOf(args.checkjstring(1).toUpperCase())));
        unicode.set("char", function(args -> {
            StringBuilder builder = new StringBuilder();
            for (int index = 1; index <= args.narg(); index++) builder.appendCodePoint(args.checkint(index));
            return LuaValue.valueOf(builder.toString());
        }));
        unicode.set("len", function(args -> {
            String value = args.checkjstring(1);
            return LuaValue.valueOf(value.codePointCount(0, value.length()));
        }));
        unicode.set("reverse", function(args -> LuaValue.valueOf(ExtendedUnicodeHelper.reverse(args.checkjstring(1)))));
        unicode.set("sub", function(args -> {
            String value = args.checkjstring(1);
            int length = ExtendedUnicodeHelper.length(value);
            int startIndex = args.checkint(2);
            int start = startIndex < 0
                ? value.offsetByCodePoints(value.length(), Math.max(startIndex, -length))
                : value.offsetByCodePoints(0, Math.min(Math.max(startIndex - 1, 0), length));
            int end;
            if (args.narg() > 2) {
                int endIndex = args.checkint(3);
                end = endIndex < 0
                    ? value.offsetByCodePoints(value.length(), Math.max(endIndex + 1, -length))
                    : value.offsetByCodePoints(0, Math.min(endIndex, length));
            } else end = value.length();
            return LuaValue.valueOf(end <= start ? "" : value.substring(start, end));
        }));
        unicode.set("isWide", function(args -> LuaValue.valueOf(FontUtils.wcwidth(args.checkjstring(1).codePointAt(0)) > 1)));
        unicode.set("charWidth", function(args -> LuaValue.valueOf(FontUtils.wcwidth(args.checkjstring(1).codePointAt(0)))));
        unicode.set("wlen", function(args -> LuaValue.valueOf(args.checkjstring(1).codePoints().map(FontUtils::wcwidth).sum())));
        unicode.set("wtrunc", function(args -> LuaValue.valueOf(FontUtils.wtrunc(args.checkjstring(1), args.checkint(2)))));
        lua().set("unicode", unicode);
    }

    private static VarArgFunction function(Function<Varargs, LuaValue> handler) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return handler.apply(args);
            }
        };
    }
}
