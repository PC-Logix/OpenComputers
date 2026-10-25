package li.cil.oc.server.machine.luaj;

import li.cil.oc.Settings;
import li.cil.repack.org.luaj.vm2.LuaValue;
import li.cil.repack.org.luaj.vm2.Varargs;
import li.cil.repack.org.luaj.vm2.lib.VarArgFunction;

public class SystemAPI extends LuaJAPI {
    public SystemAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        LuaValue system = LuaValue.tableOf();
        system.set("allowBytecode", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(Settings.get().allowBytecode());
            }
        });
        system.set("allowGC", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(Settings.get().allowGC());
            }
        });
        system.set("timeout", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(Settings.get().timeout());
            }
        });
        lua().set("system", system);
    }
}
