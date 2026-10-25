package li.cil.oc.common.blockentity.traits;

import li.cil.oc.client.Sound;
import net.minecraft.world.level.block.entity.BlockEntity;
import scala.jdk.javaapi.OptionConverters;

/** Client-side sound policy for computer running-state transitions. */
public final class ComputerSoundJava {
    private ComputerSoundJava() { }

    public static void onRunningStateChanged(Computer computer) {
        var sound = OptionConverters.toJava(computer.runSound());
        if (sound.isEmpty()) return;

        BlockEntity blockEntity = (BlockEntity) computer;
        if (computer.isRunning()) {
            long delay = 50L + blockEntity.getLevel().getRandom().nextInt(50);
            Sound.startLoop(blockEntity, sound.get(), 0.5f, delay);
        } else {
            Sound.stopLoop(blockEntity);
        }
    }

    public static void startLoopAfterClientLoad(Computer computer) {
        var sound = OptionConverters.toJava(computer.runSound());
        if (sound.isEmpty() || !computer.isRunning()) return;

        BlockEntity blockEntity = (BlockEntity) computer;
        long delay = 1000L + blockEntity.getLevel().getRandom().nextInt(2000);
        Sound.startLoop(blockEntity, sound.get(), 0.5f, delay);
    }
}
