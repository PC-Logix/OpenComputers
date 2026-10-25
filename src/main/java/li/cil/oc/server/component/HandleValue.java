package li.cil.oc.server.component;

import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.prefab.AbstractValue;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

/** Public so saved Lua userdata can reconstruct this value reflectively. */
public final class HandleValue extends AbstractValue {
    private static final String OWNER_TAG = "owner";
    private static final String HANDLE_TAG = "handle";
    private String owner = "";
    private int handle;

    public HandleValue() { }
    public HandleValue(String owner, int handle) { this.owner = owner; this.handle = handle; }
    public String owner() { return owner; }
    public void owner_$eq(String value) { owner = value; }
    public int handle() { return handle; }
    public void handle_$eq(int value) { handle = value; }

    @Override
    public void dispose(Context context) {
        super.dispose(context);
        if (context.node() == null || context.node().network() == null) return;
        Node node = context.node().network().node(owner);
        if (node != null && node.host() instanceof FileSystem fs) {
            try { fs.close(context, handle); } catch (Throwable ignored) { }
        }
    }

    @Override
    public void loadData(DataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadData(holder, nbt, provider);
        owner = nbt.getString(OWNER_TAG);
        handle = nbt.getInt(HANDLE_TAG);
    }

    @Override
    public void saveData(MutableDataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveData(holder, nbt, provider);
        nbt.putString(OWNER_TAG, owner);
        nbt.putInt(HANDLE_TAG, handle);
    }

    @Override public String toString() { return Integer.toString(handle); }
}
