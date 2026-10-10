package li.cil.oc.server.component;

import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.prefab.AbstractValue;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

public final class AudioHandleValue extends AbstractValue {
    public String owner = "";
    public int handle;

    public AudioHandleValue() {
    }

    public AudioHandleValue(String owner, int handle) {
        this.owner = owner;
        this.handle = handle;
    }

    @Override
    public void dispose(Context context) {
        super.dispose(context);
        if (context.node() != null && context.node().network() != null) {
            Node node = context.node().network().node(owner);
            if (node != null && node.host() instanceof AudioCard card) {
                try {
                    card.closeHandle(owner, handle);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Override
    public void loadData(DataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadData(holder, nbt, provider);
        owner = nbt.getString("owner");
        handle = nbt.getInt("handle");
    }

    @Override
    public void saveData(MutableDataComponentHolder holder, CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveData(holder, nbt, provider);
        nbt.putString("owner", owner);
        nbt.putInt("handle", handle);
    }

    @Override
    public String toString() {
        return Integer.toString(handle);
    }
}
