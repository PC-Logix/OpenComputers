package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Node;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Component lifecycle phases shared by Java block entities and the retained environment adapter. */
public final class ComponentInventoryLifecycleJava {
    private ComponentInventoryLifecycleJava() { }
    public static void initialize(Object host) {
        if (host instanceof ComponentInventory inventory) {
            if (inventory.isClient()) inventory.connectComponents();
            ComputerLifecycleJava.afterInitialize(host);
        }
    }
    public static void dispose(Object host) {
        if (host instanceof ComponentInventory inventory) {
            if (inventory.isClient()) inventory.disconnectComponents();
            ComputerLifecycleJava.afterDispose(host);
        }
    }
    public static void onConnect(Object host, Node node) {
        if (host instanceof ComponentInventory inventory && node == inventory.node()) inventory.connectComponents();
    }
    public static void onDisconnect(Object host, Node node) {
        if (host instanceof ComponentInventory inventory && node == inventory.node()) inventory.disconnectComponents();
    }
    public static void beforeSaveClient(Object host) {
        if (host instanceof ComponentInventory inventory) inventory.connectComponents();
    }
    public static void afterSaveClient(Object host, CompoundTag nbt, HolderLookup.Provider provider) {
        if (host instanceof ComponentInventory inventory) {
            inventory.saveData(nbt, provider);
            RedstoneLifecycleJava.saveClient(host, true, nbt);
        }
    }
    public static void afterLoadClient(Object host, CompoundTag nbt, HolderLookup.Provider provider) {
        if (host instanceof ComponentInventory inventory) {
            try { inventory.loadData(nbt, provider); }
            catch (li.cil.oc.api.UnrecoverablePersistanceException error) { ComponentInventoryLifecycleJava.<RuntimeException>throwUnchecked(error); }
            inventory.connectComponents();
            RedstoneLifecycleJava.loadClient(host, true, nbt);
        }
    }
    @SuppressWarnings("unchecked") private static <E extends Throwable> void throwUnchecked(Throwable error) throws E { throw (E) error; }
}
