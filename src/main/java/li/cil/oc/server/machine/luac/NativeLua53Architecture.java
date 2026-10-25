package li.cil.oc.server.machine.luac;

import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Machine;

@Architecture.Name("Lua 5.3")
public final class NativeLua53Architecture extends NativeLuaArchitecture {
    public NativeLua53Architecture(Machine machine) { super(machine); }
    @Override protected LuaStateFactoryBase factory() { return LuaStateFactory.Lua53; }
}
