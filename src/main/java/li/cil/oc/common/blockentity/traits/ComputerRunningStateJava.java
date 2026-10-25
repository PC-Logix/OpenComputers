package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Applies requested computer running-state changes without embedding sound policy. */
public final class ComputerRunningStateJava {
    private ComputerRunningStateJava() { }

    public static void setRunning(Computer computer, boolean current, boolean value) {
        if (current == value) return;

        computer.setRequestedRunningState(value);
        BlockEntity blockEntity = (BlockEntity) computer;
        Level level = blockEntity.getLevel();
        if (level == null || computer.isMoving()) return;

        BlockPos position = blockEntity.getBlockPos();
        BlockState state = level.getBlockState(position);
        level.sendBlockUpdated(position, state, state, 3);
        if (level.isClientSide()) ComputerSoundJava.onRunningStateChanged(computer);
    }
}
