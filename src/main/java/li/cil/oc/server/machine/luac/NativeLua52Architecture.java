package li.cil.oc.server.machine.luac;

import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Machine;

@Architecture.Name("Lua 5.2")
public final class NativeLua52Architecture extends NativeLuaArchitecture {
    public NativeLua52Architecture(Machine machine) { super(machine); }
    @Override protected LuaStateFactoryBase factory() { return LuaStateFactory.Lua52; }
}
