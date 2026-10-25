package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Root phases shared by the native base and legacy Scala super-call adapters. */
public final class BaseBlockEntityPhasesJava {
    private BaseBlockEntityPhasesJava() { }
    public static void update(BaseBlockEntity host) {
        TextBufferLifecycleJava.ensureLoaded(host);
        ComputerLifecycleJava.beforeUpdate(host);
        BaseBlockEntityLifecycleJava.updateEntity(host);
        PowerAcceptorLifecycleJava.update(host, false);
        RedstoneLifecycleJava.update(host, false);
    }
    public static void initialize(BaseBlockEntity host) { PowerAcceptorLifecycleJava.initialize(host, false); }
    public static void dispose(BaseBlockEntity host) {
        PowerAcceptorLifecycleJava.dispose(host);
        if (host.isClient()) li.cil.oc.client.Sound.stopLoop((net.minecraft.world.level.block.entity.BlockEntity) host);
    }
    public static void loadServer(BaseBlockEntity host, CompoundTag nbt, HolderLookup.Provider provider) {
        PowerAcceptorLifecycleJava.load(host, false, nbt, provider);
        RedstoneLifecycleJava.loadServer(host, RedstoneLifecycleJava.Phase.BASE, nbt);
    }
    public static void saveServer(BaseBlockEntity host, CompoundTag nbt, HolderLookup.Provider provider) {
        TextBufferLifecycleJava.ensureLoaded(host);
        nbt.putBoolean(li.cil.oc.SettingsAccessJava.namespace() + "isServerData", true);
        PowerAcceptorLifecycleJava.save(host, false, nbt, provider);
        RedstoneLifecycleJava.saveServer(host, RedstoneLifecycleJava.Phase.BASE, nbt);
    }
    public static void loadClient(BaseBlockEntity host, CompoundTag nbt, HolderLookup.Provider provider) {
        PowerInformationJava.loadHostForClient(host, nbt);
        TextBufferLifecycleJava.loadClient(host, nbt, provider);
        RedstoneLifecycleJava.loadClient(host, false, nbt);
        ComponentInventoryLifecycleJava.afterLoadClient(host, nbt, provider);
    }
    public static void saveClient(BaseBlockEntity host, CompoundTag nbt, HolderLookup.Provider provider) {
        ComponentInventoryLifecycleJava.beforeSaveClient(host);
        nbt.putBoolean(li.cil.oc.SettingsAccessJava.namespace() + "isServerData", false);
        PowerInformationJava.saveHostForClient(host, nbt);
        TextBufferLifecycleJava.saveClient(host, nbt);
        RedstoneLifecycleJava.saveClient(host, false, nbt);
        ComponentInventoryLifecycleJava.afterSaveClient(host, nbt, provider);
    }
}
