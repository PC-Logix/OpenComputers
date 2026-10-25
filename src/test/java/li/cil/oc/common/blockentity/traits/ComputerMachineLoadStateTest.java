package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public final class ComputerMachineLoadStateTest {
    @Test
    public void storesAndConsumesDeferredMachineDataOnce() {
        ComputerMachineLoadState state = new ComputerMachineLoadState();
        DataComponentMap data = DataComponentMap.EMPTY;

        state.deferMachineData(data);

        assertSame(data, state.takePendingMachineData());
        assertNull(state.takePendingMachineData());
    }

    @Test
    public void savesAndRestoresMovingMachineGrace() {
        ComputerMachineLoadState state = new ComputerMachineLoadState();
        CompoundTag saved = new CompoundTag();

        state.saveMovingState(saved);
        state.loadAdditional(saved);

        assertEquals(40, state.pendingMovingRestoreGrace());
    }
}
