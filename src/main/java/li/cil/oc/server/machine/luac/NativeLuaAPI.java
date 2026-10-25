package li.cil.oc.server.machine.luac;

import li.cil.oc.server.machine.ArchitectureAPI;
import li.cil.repack.com.naef.jnlua.LuaState;

public abstract class NativeLuaAPI extends ArchitectureAPI {
    private final NativeLuaArchitecture owner;

    protected NativeLuaAPI(NativeLuaArchitecture owner) {
        super(owner.machine());
        this.owner = owner;
    }

    public NativeLuaArchitecture owner() {
        return owner;
    }

    protected LuaState lua() {
        return owner.lua();
    }
}
