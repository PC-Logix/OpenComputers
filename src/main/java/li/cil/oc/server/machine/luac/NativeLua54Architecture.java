package li.cil.oc.server.machine.luac;

import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Machine;

@Architecture.Name("Lua 5.4")
public final class NativeLua54Architecture extends NativeLuaArchitecture {
    public NativeLua54Architecture(Machine machine) { super(machine); }
    @Override protected LuaStateFactoryBase factory() { return LuaStateFactory.Lua54; }
}
