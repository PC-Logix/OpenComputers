package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class InventoryRuntimeJavaTest {
    @Test
    public void lazilyCreatesAndReusesEmptySlots() {
        InventoryRuntimeJava state = new InventoryRuntimeJava();
        ItemStack[] first = state.items(3, null);

        assertArrayEquals(new ItemStack[]{null, null, null}, first);
        assertSame(first, state.items(3, null));
    }

    @Test
    public void checksDistanceFromMovingPositionOrBlockCenter() {
        assertTrue(InventoryRuntimeJava.stillValid(
                new Vec3(0, 0, 0), true, new Vec3(8, 0, 0), BlockPos.ZERO));
        assertFalse(InventoryRuntimeJava.stillValid(
                new Vec3(0, 0, 0), true, new Vec3(8.001, 0, 0), BlockPos.ZERO));

        assertTrue(InventoryRuntimeJava.stillValid(
                new Vec3(8.5, 0.5, 0.5), false, Vec3.ZERO, BlockPos.ZERO));
        assertFalse(InventoryRuntimeJava.stillValid(
                new Vec3(8.5001, 0.5, 0.5), false, Vec3.ZERO, BlockPos.ZERO));
    }
}
