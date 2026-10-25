package li.cil.oc.common.blockentity.traits;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.datacomponents.MutableNbtComponentHolder;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.common.blockentity.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Builds block-entity update snapshots with their matching component patch. */
final class BaseBlockEntityUpdateTagJava {
    private BaseBlockEntityUpdateTagJava() { }

    static CompoundTag getUpdateTag(BlockEntity entity, BaseBlockEntity self, CompoundTag nbt,
                                    HolderLookup.Provider provider) {
        SaveHandler.savingForClients = true;
        try {
            try {
                if (entity instanceof Screen screen) screen.saveForClientDirect(nbt, provider);
                else self.saveForClient(nbt, provider);
            } catch (Throwable error) {
                OpenComputers.log().warn("There was a problem writing a BlockEntity description packet. Please report this if you see it!", error);
            }

            try {
                MutableNbtComponentHolder holder = new MutableNbtComponentHolder(nbt, provider);
                self.saveComponentsCommon(holder);
                self.saveComponentsForClient(holder);
                holder.save(nbt, provider);
            } catch (Throwable error) {
                OpenComputers.log().warn("There was a problem writing BlockEntity client components. Please report this if you see it!", error);
            }
        } finally {
            SaveHandler.savingForClients = false;
        }

        return nbt;
    }
}
