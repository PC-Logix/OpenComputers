package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

/** Computer callbacks retain their position around the shared environment and inventory phases. */
public final class ComputerLifecycleJava {
    private ComputerLifecycleJava() { }
    public static void beforeUpdate(Object host) { if (host instanceof Computer computer) ComputerRuntimeJava.update(computer); }
    public static void afterInitialize(Object host) { if (host instanceof Computer computer) computer.computerState().load.initialize(computer); }
    public static void afterDispose(Object host) { if (host instanceof Computer computer) ComputerRuntimeJava.afterDispose(computer); }
    public static void beforeLoadAdditional(Object host, CompoundTag nbt) { if (host instanceof Computer computer) computer.computerState().load.loadAdditional(nbt); }
    public static void afterLoadComponentsForServer(Object host, DataComponentHolder holder) {
        if (host instanceof Computer computer) computer.computerState().load.loadComponentsForServer(computer, holder);
    }
    public static void afterSaveComponentsForServer(Object host, MutableDataComponentHolder holder) {
        if (host instanceof Computer computer) ComputerComponentDataJava.saveForServer(computer, holder);
    }
    public static void loadComponentsForClient(Object host, DataComponentHolder holder) {
        if (host instanceof Computer computer) {
            ComputerComponentDataJava.loadForClient(computer, holder);
            ComputerSoundJava.startLoopAfterClientLoad(computer);
        }
    }
    public static void saveComponentsForClient(Object host, MutableDataComponentHolder holder) {
        if (host instanceof Computer computer) ComputerComponentDataJava.saveForClient(computer, holder);
    }
    public static void afterSetChanged(Object host) { if (host instanceof Computer computer) ComputerRuntimeJava.afterSetChanged(computer); }
    public static boolean canInteract(Object host, Player player) {
        return !(host instanceof Computer computer) || computer.computerState().components.canInteract(computer, player);
    }
    public static void afterSaveMovingState(Object host, CompoundTag nbt) {
        if (host instanceof Computer computer) computer.computerState().load.saveMovingState(nbt);
    }
}
