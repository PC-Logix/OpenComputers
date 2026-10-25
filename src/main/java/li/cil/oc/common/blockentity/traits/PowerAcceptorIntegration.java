package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface PowerAcceptorIntegration {
    void initialize(PowerAcceptor host);

    void update(PowerAcceptor host);

    void dispose(PowerAcceptor host);

    void load(PowerAcceptor host, CompoundTag nbt, HolderLookup.Provider provider);

    void save(PowerAcceptor host, CompoundTag nbt, HolderLookup.Provider provider);
}
