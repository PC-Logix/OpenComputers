package li.cil.oc.server.machine.luaj;

import li.cil.oc.util.GameTimeFormatter;
import li.cil.oc.util.GameTimeFormatter.DateTime;
import li.cil.repack.org.luaj.vm2.LuaTable;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;
import li.cil.repack.org.luaj.vm2.lib.VarArgFunction;

import java.util.function.Function;

public class OSAPI extends LuaJAPI {
    public OSAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        LuaValue os = LuaValue.tableOf();
        os.set("clock", function(args -> LuaValue.valueOf(machine.cpuTime())));
        os.set("date", function(args -> {
            String format = args.narg() > 0 && args.isstring(1) ? args.tojstring(1) : "%d/%m/%y %H:%M:%S";
            double time = args.narg() > 1 && args.isnumber(2)
                ? args.todouble(2) : (double) ((machine.worldTime() + 6000) * 60 * 60 / 1000);
            DateTime dateTime = GameTimeFormatter.parse(time);
            if (format.startsWith("!")) format = format.substring(1);
            if (format.equals("*t")) {
                LuaValue table = LuaValue.tableOf(0, 8);
                table.set("year", LuaValue.valueOf(dateTime.year()));
                table.set("month", LuaValue.valueOf(dateTime.month()));
                table.set("day", LuaValue.valueOf(dateTime.day()));
                table.set("hour", LuaValue.valueOf(dateTime.hour()));
                table.set("min", LuaValue.valueOf(dateTime.minute()));
                table.set("sec", LuaValue.valueOf(dateTime.second()));
                table.set("wday", LuaValue.valueOf(dateTime.weekDay()));
                table.set("yday", LuaValue.valueOf(dateTime.yearDay()));
                return table;
            }
            return LuaValue.valueOf(GameTimeFormatter.format(format, dateTime));
        }));
        os.set("time", function(args -> {
            if (args.isnoneornil(1)) {
                return LuaValue.valueOf((double) ((machine.worldTime() + 6000) * 60 * 60 / 1000));
            }
            LuaTable table = args.checktable(1);
            int second = field(table, "sec", 0);
            int minute = field(table, "min", 0);
            int hour = field(table, "hour", 12);
            int day = field(table, "day", -1);
            int month = field(table, "month", -1);
            int year = field(table, "year", -1);
            return LuaValue.valueOf(GameTimeFormatter.mktime(year, month, day, hour, minute, second));
        }));
        lua().set("os", os);
    }

    private static int field(LuaTable table, String key, int fallback) {
        LuaValue value = table.get(key);
        if (value.isint()) return value.toint();
        if (fallback < 0) throw unchecked(new Exception("field '" + key + "' missing in date table"));
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException unchecked(Throwable error) throws T {
        throw (T) error;
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
