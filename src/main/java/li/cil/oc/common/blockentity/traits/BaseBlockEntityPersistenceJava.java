package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.Persistable;
import li.cil.oc.api.datacomponents.MutableNbtComponentHolder;
import li.cil.oc.api.datacomponents.NbtComponentHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Component/NBT orchestration shared by Scala-mixed block entities. */
final class BaseBlockEntityPersistenceJava {
    private static final String IS_SERVER_DATA_TAG = li.cil.oc.Settings.namespace() + "isServerData";

    private BaseBlockEntityPersistenceJava() { }

    static void loadAdditional(BaseBlockEntity self, CompoundTag nbt, HolderLookup.Provider provider,
                               boolean isServer) {
        if (isServer || nbt.getBoolean(IS_SERVER_DATA_TAG)) self.loadForServer(nbt, provider);
        else self.loadForClient(nbt, provider);
    }

    static void loadWithComponents(BlockEntity entity, BaseBlockEntity self, CompoundTag tag,
                                   HolderLookup.Provider registries, boolean isServer) {
        DataComponentHolder holder;
        if (isServer) {
            // Persistent block entities restore their serialized patch; newly placed blocks use their live stack map.
            holder = NbtComponentHolder.hasComponents(tag)
                ? new NbtComponentHolder(tag, registries)
                : Persistable.holder(entity);
        } else {
            holder = new NbtComponentHolder(tag, registries);
        }

        self.loadComponentsCommon(holder);
        if (isServer) self.loadComponentsForServer(holder);
        else self.loadComponentsForClient(holder);
    }

    static void saveComponents(BlockEntity entity, BaseBlockEntity self, CompoundTag nbt,
                               HolderLookup.Provider provider, boolean isServer) {
        if (isServer) {
            // Seed from server NBT so saveForServer component writes survive this final merged patch.
            MutableNbtComponentHolder holder = new MutableNbtComponentHolder(nbt, provider);
            self.saveComponentsCommon(holder);
            self.saveComponentsForServer(holder);
            holder.save(nbt, provider);

            // Keep Minecraft's live component map current for item transfers.
            Persistable.BlockEntityMutableComponentHolder liveHolder = Persistable.holder(entity);
            try {
                liveHolder.applyComponents(holder.getComponents());
            } finally {
                closeUnchecked(liveHolder);
            }
        } else {
            Persistable.BlockEntityMutableComponentHolder holder = Persistable.holder(entity);
            try {
                self.saveComponentsCommon(holder);
                self.saveComponentsForClient(holder);
            } finally {
                closeUnchecked(holder);
            }
        }
    }

    private static void closeUnchecked(AutoCloseable holder) {
        try {
            holder.close();
        } catch (Exception exception) {
            BaseBlockEntityPersistenceJava.<RuntimeException>throwUnchecked(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
