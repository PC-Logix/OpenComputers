package li.cil.oc.server.machine.luac;

import li.cil.oc.Settings;
import li.cil.repack.com.naef.jnlua.LuaState;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class PersistenceAPI extends NativeLuaAPI {
    private String persistKey = "__persist" + UUID.randomUUID().toString().replace("-", "");

    public PersistenceAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    public String currentPersistKey() {
        return persistKey;
    }

    @Override
    public void initialize() {
        lua().pushJavaFunction(state -> {
            state.pushString(persistKey);
            return 1;
        });
        lua().setGlobal("persistKey");

        if (Settings.get().allowPersistence()) {
            // Values reachable from _G must have stable names across saves.
            lua().newTable(); // perms
            lua().newTable(); // uperms
            int perms = lua().getTop() - 1;
            int uperms = lua().getTop();
            lua().pushString("_G");
            lua().getGlobal("_G");
            flattenAndStore(perms, uperms);
            lua().setField(lua().getRegistryIndex(), "uperms");
            lua().setField(lua().getRegistryIndex(), "perms");
        }
    }

    // Stack on entry: ... key value. Consumes both values before returning.
    private void flattenAndStore(int perms, int uperms) {
        if (lua().isFunction(-1) || lua().isTable(-1)) {
            lua().pushValue(-2);
            lua().getTable(uperms);
            if (!lua().isNil(-1)) throw new AssertionError("duplicate permanent value named " + lua().toString(-3));
            lua().pop(1);

            lua().pushValue(-1);
            lua().getTable(perms);
            boolean isNew = lua().isNil(-1);
            lua().pop(1);
            if (isNew) {
                lua().pushValue(-1);
                lua().pushValue(-3);
                lua().rawSet(perms);
                lua().pushValue(-2);
                lua().pushValue(-2);
                lua().rawSet(uperms);

                if (lua().isTable(-1)) {
                    String key = lua().toString(-2);
                    ArrayList<String> childKeys = new ArrayList<>();
                    lua().pushNil();
                    while (lua().next(-2)) {
                        lua().pop(1);
                        childKeys.add(lua().toString(-1));
                    }
                    Collections.sort(childKeys);
                    for (String childKey : childKeys) {
                        lua().pushString(key + "." + childKey);
                        lua().getField(-2, childKey);
                        flattenAndStore(perms, uperms);
                    }
                }
            }
        }
        lua().pop(2);
    }

    @Override
    public void loadData(CompoundTag tag) {
        super.loadData(tag);
        if (tag.contains("persistKey")) persistKey = tag.getString("persistKey");
    }

    @Override
    public void saveData(CompoundTag tag) {
        super.saveData(tag);
        tag.putString("persistKey", persistKey);
    }

    public void configure() {
        lua().getGlobal("eris");
        lua().getField(-1, "settings");
        lua().pushString("spkey");
        lua().pushString(persistKey);
        lua().call(2, 0);
        lua().getField(-1, "settings");
        lua().pushString("path");
        lua().pushBoolean(Settings.get().debugPersistence());
        lua().call(2, 0);
        lua().pop(1);
    }

    public byte[] persist(int index) {
        if (Settings.get().allowPersistence()) {
            configure();
            try {
                lua().gc(LuaState.GcAction.STOP, 0);
                lua().getGlobal("eris");
                lua().getField(-1, "persist");
                if (lua().isFunction(-1)) {
                    lua().getField(lua().getRegistryIndex(), "perms");
                    lua().pushValue(index);
                    try {
                        lua().call(2, 1);
                    } catch (Throwable error) {
                        lua().pop(1);
                        throw unchecked(error);
                    }
                    if (lua().isString(-1)) {
                        byte[] result = lua().toByteArray(-1);
                        lua().pop(2);
                        return result;
                    }
                }
                lua().pop(2);
            } finally {
                lua().gc(LuaState.GcAction.RESTART, 0);
            }
        }
        return new byte[0];
    }

    public boolean unpersist(byte[] value) {
        if (Settings.get().allowPersistence()) {
            configure();
            try {
                lua().gc(LuaState.GcAction.STOP, 0);
                lua().getGlobal("eris");
                lua().getField(-1, "unpersist");
                if (lua().isFunction(-1)) {
                    lua().getField(lua().getRegistryIndex(), "uperms");
                    lua().pushByteArray(value);
                    lua().call(2, 1);
                    lua().insert(-2);
                    lua().pop(1);
                    return true;
                }
                lua().pop(1);
            } finally {
                lua().gc(LuaState.GcAction.RESTART, 0);
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException unchecked(Throwable error) throws T {
        throw (T) error;
    }
}
