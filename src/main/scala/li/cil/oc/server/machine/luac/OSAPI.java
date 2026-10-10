package li.cil.oc.server.machine.luac;

import li.cil.oc.util.GameTimeFormatter;
import li.cil.oc.util.GameTimeFormatter.DateTime;
import li.cil.repack.com.naef.jnlua.JavaFunction;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaType;

public class OSAPI extends NativeLuaAPI {
    public OSAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        lua().getGlobal("os");
        add("clock", state -> {
            state.pushNumber(machine.cpuTime());
            return 1;
        });
        add("date", state -> {
            String format = state.getTop() > 0 && state.isString(1) ? state.toString(1) : "%d/%m/%y %H:%M:%S";
            double time = state.getTop() > 1 && state.isNumber(2)
                ? state.toNumber(2) : ((machine.worldTime() + 6000) * 60 * 60 / 1000.0);
            DateTime dateTime = GameTimeFormatter.parse(time);
            if (format.startsWith("!")) format = format.substring(1);
            if (format.equals("*t")) {
                state.newTable(0, 8);
                dateField(state, "year", dateTime.year());
                dateField(state, "month", dateTime.month());
                dateField(state, "day", dateTime.day());
                dateField(state, "hour", dateTime.hour());
                dateField(state, "min", dateTime.minute());
                dateField(state, "sec", dateTime.second());
                dateField(state, "wday", dateTime.weekDay());
                dateField(state, "yday", dateTime.yearDay());
            } else {
                state.pushString(GameTimeFormatter.format(format, dateTime));
            }
            return 1;
        });
        add("time", state -> {
            if (state.isNoneOrNil(1)) {
                state.pushNumber(((machine.worldTime() + 6000) * 60 * 60 / 1000.0));
            } else {
                state.checkType(1, LuaType.TABLE);
                state.setTop(1);
                int second = field(state, "sec", 0);
                int minute = field(state, "min", 0);
                int hour = field(state, "hour", 12);
                int day = field(state, "day", -1);
                int month = field(state, "month", -1);
                int year = field(state, "year", -1);
                state.pushNumber(GameTimeFormatter.mktime(year, month, day, hour, minute, second));
            }
            return 1;
        });
        lua().pop(1);
    }

    private void add(String name, JavaFunction function) {
        lua().pushJavaFunction(function);
        lua().setField(-2, name);
    }

    private static void dateField(LuaState state, String name, int value) {
        state.pushInteger(value);
        state.setField(-2, name);
    }

    private static int field(LuaState state, String name, int fallback) {
        state.getField(-1, name);
        Long value = state.toIntegerX(-1);
        state.pop(1);
        if (value != null) return value.intValue();
        if (fallback < 0) throw unchecked(new Exception("field '" + name + "' missing in date table"));
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException unchecked(Throwable error) throws T {
        throw (T) error;
    }
}
