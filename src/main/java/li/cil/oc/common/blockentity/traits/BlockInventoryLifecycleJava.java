package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.Persistable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

/** Inventory persistence phases for native Java block-entity hosts. */
public final class BlockInventoryLifecycleJava {
    private BlockInventoryLifecycleJava() { }
    public static void loadServer(BlockInventory inventory, CompoundTag nbt, HolderLookup.Provider provider) {
        try {
            if (!InventoryStateJava.load(inventory.items(), nbt, provider, tag(), inventory::updateItems)) inventory.loadData(nbt, provider);
        } catch (li.cil.oc.api.UnrecoverablePersistanceException error) { BlockInventoryLifecycleJava.<RuntimeException>throwUnchecked(error); }
        RedstoneLifecycleJava.loadServer(inventory, RedstoneLifecycleJava.Phase.INVENTORY, nbt);
    }
    public static void saveServer(BlockInventory inventory, CompoundTag nbt, HolderLookup.Provider provider) {
        InventoryStateJava.save(inventory.items(), nbt, provider, tag());
        RedstoneLifecycleJava.saveServer(inventory, RedstoneLifecycleJava.Phase.INVENTORY, nbt);
    }
    public static void loadComponents(BlockInventory inventory, DataComponentHolder holder) {
        inventory.loadData(Persistable.holder((net.minecraft.world.level.block.entity.BlockEntity) inventory));
        ComputerLifecycleJava.afterLoadComponentsForServer(inventory, holder);
    }
    public static void saveComponents(BlockInventory inventory, MutableDataComponentHolder holder) {
        var inner = Persistable.holder((net.minecraft.world.level.block.entity.BlockEntity) inventory);
        try { inventory.saveData(inner); }
        finally {
            try { inner.close(); }
            catch (Exception error) { BlockInventoryLifecycleJava.<RuntimeException>throwUnchecked(error); }
        }
        ComputerLifecycleJava.afterSaveComponentsForServer(inventory, holder);
    }
    private static String tag() { return li.cil.oc.SettingsAccessJava.namespace() + "items"; }
    @SuppressWarnings("unchecked") private static <E extends Throwable> void throwUnchecked(Throwable error) throws E { throw (E) error; }
}
