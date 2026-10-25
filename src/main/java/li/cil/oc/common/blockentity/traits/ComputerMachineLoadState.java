package li.cil.oc.common.blockentity.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.Persistable;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.common.blockentity.RobotProxy;
import li.cil.oc.server.machine.Machine;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;

/** Owns the machine data that must wait until its block entity has a live level. */
public final class ComputerMachineLoadState {
    private static final String MOVING_RESTORE_GRACE_TAG = Settings.namespace() + "movingRestoreGrace";

    private DataComponentMap pendingMachineData;
    private int pendingMovingRestoreGrace;

    public void loadAdditional(CompoundTag nbt) {
        pendingMovingRestoreGrace = nbt.getInt(MOVING_RESTORE_GRACE_TAG);
    }

    public void saveMovingState(CompoundTag nbt) {
        // The regular chunk save writes fresh NBT after the restored machine
        // consumes this one-shot component-restoration grace period.
        nbt.putInt(MOVING_RESTORE_GRACE_TAG, 40);
    }

    public void deferMachineData(DataComponentMap data) {
        pendingMachineData = data;
    }

    public DataComponentMap takePendingMachineData() {
        DataComponentMap data = pendingMachineData;
        pendingMachineData = null;
        return data;
    }

    int pendingMovingRestoreGrace() {
        return pendingMovingRestoreGrace;
    }

    public void loadComponentsForServer(Computer computer, DataComponentHolder holder) {
        Level level = ((BlockEntity) computer).getLevel();

        // Robot machine data is hosted by a proxy while the robot entity may
        // still have stale coordinates from its previous attachment.
        if (computer instanceof RobotProxy proxy) proxy.robot().setLevel(level);

        // Minecraft deserializes components before it attaches the level. The
        // machine needs registry and dimension state, so defer only this load.
        if (level == null) {
            deferMachineData(holder.getComponents());
        } else {
            loadMachineData(computer, holder);
        }
    }

    public void initialize(Computer computer) {
        if (!computer.isServer() || ((BlockEntity) computer).getLevel() == null || pendingMachineData == null) return;

        DataComponentMap data = takePendingMachineData();
        loadMachineData(computer, Persistable.holder(data));
    }

    private void loadMachineData(Computer computer, DataComponentHolder holder) {
        try {
            computer.machine().loadData(holder);
        } catch (UnrecoverablePersistanceException error) {
            ComputerMachineLoadState.<RuntimeException>throwUnchecked(error);
        }

        if (pendingMovingRestoreGrace > 0) {
            if (computer.machine() instanceof Machine implementation) {
                implementation.beginComponentRestoreGrace(pendingMovingRestoreGrace);
            }
            pendingMovingRestoreGrace = 0;
        }

        // Preserve the initialization order used to prevent a later client
        // component packet from overwriting the machine's loaded running state.
        computer.setRunning(computer.machine().isRunning());
        computer._isOutputEnabled_$eq(computer.hasRedstoneCard());
    }

    private static <T extends Throwable> void throwUnchecked(Throwable error) throws T {
        throw (T) error;
    }
}
