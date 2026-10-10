package li.cil.oc.server.machine.luaj;

import li.cil.oc.server.machine.ArchitectureAPI;
import li.cil.repack.org.luaj.vm2.Globals;

public abstract class LuaJAPI extends ArchitectureAPI {
    private final LuaJLuaArchitecture owner;

    protected LuaJAPI(LuaJLuaArchitecture owner) {
        super(owner.machine());
        this.owner = owner;
    }

    public LuaJLuaArchitecture owner() {
        return owner;
    }

    protected Globals lua() {
        return owner.lua();
    }
}
